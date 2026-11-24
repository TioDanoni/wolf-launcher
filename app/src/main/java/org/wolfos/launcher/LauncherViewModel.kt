package org.wolfos.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wolfos.launcher.atualizacao.Atualizador
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.atualizacao.TrabalhoAtualizacao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.EscolhasVerbo
import org.wolfos.launcher.dados.Icones
import org.wolfos.launcher.dados.Preferencias
import org.wolfos.launcher.dados.Verbo

/** Estado do launcher: os apps (lidos fora da tela), as escolhas de verbo e a atualizacao. */
class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val escolhasArq = EscolhasVerbo(app)
    val preferencias = Preferencias(app)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _escolhas = MutableStateFlow(escolhasArq.ler())
    val escolhas: StateFlow<Map<String, String>> = _escolhas.asStateFlow()

    private val _instalarSozinho = MutableStateFlow(preferencias.instalarSozinho)
    val instalarSozinho: StateFlow<Boolean> = _instalarSozinho.asStateFlow()

    val atualizacao: StateFlow<EstadoAtualizacao> = Atualizador.estado

    // instalou, removeu ou atualizou um app: a lista acompanha
    private val pacotes = object : BroadcastReceiver() {
        override fun onReceive(contexto: Context, intent: Intent) {
            intent.data?.schemeSpecificPart?.let(Icones::esquecer)
            recarregar()
        }
    }

    init {
        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // as mensagens de pacote sao do sistema: chegam mesmo com o receptor fechado para os outros apps
        ContextCompat.registerReceiver(app, pacotes, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
        recarregar()
        TrabalhoAtualizacao.agendar(app)
    }

    fun recarregar() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { Apps.carregar(getApplication()) }
        }
    }

    fun mudarVerbo(app: AppInfo, verbo: Verbo?) {
        escolhasArq.gravar(app.chave, verbo)
        _escolhas.value = escolhasArq.ler()
    }

    /** O launcher voltou para a tela: procura versao nova se faz tempo que nao procura. */
    fun aoVoltar() {
        viewModelScope.launch {
            Atualizador.aoVoltar(getApplication())
            val passou = System.currentTimeMillis() - preferencias.ultimaVerificacao
            if (passou > SEIS_HORAS) Atualizador.verificar(getApplication())
        }
    }

    fun procurarAtualizacao(aoTerminar: (achou: Boolean) -> Unit = {}) {
        viewModelScope.launch { aoTerminar(Atualizador.verificar(getApplication()) != null) }
    }

    /** O botao do aviso: faz o que o momento pede (atualizar, dar permissao, concluir). */
    fun acaoAtualizacao() {
        val contexto = getApplication<Application>()
        when (val e = atualizacao.value) {
            is EstadoAtualizacao.Disponivel -> viewModelScope.launch { Atualizador.baixarEInstalar(contexto, e.versao) }
            is EstadoAtualizacao.Erro -> e.versao?.let { v -> viewModelScope.launch { Atualizador.baixarEInstalar(contexto, v) } }
            is EstadoAtualizacao.PrecisaPermissao -> Atualizador.pedirPermissao(contexto)
            is EstadoAtualizacao.Confirmar ->
                if (!Apps.abrirTela(contexto, e.intent)) viewModelScope.launch { Atualizador.baixarEInstalar(contexto, e.versao) }
            else -> Unit
        }
    }

    fun definirInstalarSozinho(valor: Boolean) {
        preferencias.instalarSozinho = valor
        _instalarSozinho.value = valor
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(pacotes)
    }

    private companion object {
        const val SEIS_HORAS = 6 * 60 * 60 * 1000L
    }
}
