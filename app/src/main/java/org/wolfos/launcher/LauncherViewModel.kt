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
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.avisos.Avisos
import org.wolfos.launcher.avisos.Musica
import org.wolfos.launcher.avisos.Tocando
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.atualizacao.TrabalhoAtualizacao
import org.wolfos.launcher.dados.Agenda
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.EscolhasVerbo
import org.wolfos.launcher.dados.Icones
import org.wolfos.launcher.dados.Matilha
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Preferencias
import org.wolfos.launcher.dados.Ritmo
import org.wolfos.launcher.dados.Uso
import org.wolfos.launcher.dados.Verbo

/** Estado do launcher: os apps (lidos fora da tela), as escolhas de verbo e a atualizacao. */
class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val escolhasArq = EscolhasVerbo(app)
    val preferencias = Preferencias(app)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _escolhas = MutableStateFlow(escolhasArq.ler())
    val escolhas: StateFlow<Map<String, String>> = _escolhas.asStateFlow()

    /** O que a pessoa abre e a que horas (so no celular). */
    val uso = Uso(app)

    private val _ritmo = MutableStateFlow(preferencias.ritmo)
    val ritmo: StateFlow<Boolean> = _ritmo.asStateFlow()

    private val _modoNoite = MutableStateFlow(preferencias.modoNoite)
    val modoNoite: StateFlow<Boolean> = _modoNoite.asStateFlow()

    private val _janelaNoite = MutableStateFlow(preferencias.noiteInicio to preferencias.noiteFim)
    val janelaNoite: StateFlow<Pair<Int, Int>> = _janelaNoite.asStateFlow()

    private val _fone = MutableStateFlow(false)
    val fone: StateFlow<Boolean> = _fone.asStateFlow()

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _temAgenda = MutableStateFlow(Agenda.temPermissao(app))
    val temAgenda: StateFlow<Boolean> = _temAgenda.asStateFlow()

    private val _alarme = MutableStateFlow<Long?>(null)
    val alarme: StateFlow<Long?> = _alarme.asStateFlow()

    /** Conta quantas vezes o uso mudou (para os verbos reordenarem). */
    private val _usoMudou = MutableStateFlow(0)
    val usoMudou: StateFlow<Int> = _usoMudou.asStateFlow()

    val musica: StateFlow<Musica?> = Tocando.musica

    private val _iconesNosVerbos = MutableStateFlow(preferencias.iconesNosVerbos)
    val iconesNosVerbos: StateFlow<Boolean> = _iconesNosVerbos.asStateFlow()

    fun definirIconesNosVerbos(valor: Boolean) {
        preferencias.iconesNosVerbos = valor
        _iconesNosVerbos.value = valor
    }

    private val _mostrarMatilha = MutableStateFlow(preferencias.mostrarMatilha)
    val mostrarMatilha: StateFlow<Boolean> = _mostrarMatilha.asStateFlow()

    private val _instalarSozinho = MutableStateFlow(preferencias.instalarSozinho)
    val instalarSozinho: StateFlow<Boolean> = _instalarSozinho.asStateFlow()

    val atualizacao: StateFlow<EstadoAtualizacao> = Atualizador.estado

    private val _contatos = MutableStateFlow<List<Contato>>(emptyList())
    val contatos: StateFlow<List<Contato>> = _contatos.asStateFlow()

    private val _temContatos = MutableStateFlow(Contatos.temPermissao(app))
    val temContatos: StateFlow<Boolean> = _temContatos.asStateFlow()

    /** As notificacoes (so com o acesso permitido). */
    val avisos: StateFlow<List<Aviso>> = Avisos.lista

    private val _avisosPermitidos = MutableStateFlow(Avisos.permitido(app))
    val avisosPermitidos: StateFlow<Boolean> = _avisosPermitidos.asStateFlow()

    private val matilhaArq = Matilha(app)
    private val _matilha = MutableStateFlow(matilhaArq.ler())

    /** Os ids escolhidos para a Matilha (vazio: os favoritos da agenda). */
    val matilha: StateFlow<List<Long>> = _matilha.asStateFlow()

    /** Se o WolfLauncher e a tela inicial do Android (senao, a faixa pede). */
    private val _ehPadrao = MutableStateFlow(Padrao.ehPadrao(app))
    val ehPadrao: StateFlow<Boolean> = _ehPadrao.asStateFlow()

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
        carregarContatos()
        TrabalhoAtualizacao.agendar(app)
    }

    /** Le a agenda fora da tela (so se a pessoa deixou). */
    fun carregarContatos() {
        val contexto = getApplication<Application>()
        _temContatos.value = Contatos.temPermissao(contexto)
        if (!_temContatos.value) return
        viewModelScope.launch {
            _contatos.value = withContext(Dispatchers.IO) { Contatos.carregar(contexto) }
        }
    }

    fun recarregar() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { Apps.carregar(getApplication()) }
        }
    }

    /** O fone, a agenda e o despertador: o que muda o "agora". */
    fun carregarDia() {
        val contexto = getApplication<Application>()
        _fone.value = Ritmo.foneConectado(contexto)
        _alarme.value = Ritmo.proximoAlarme(contexto)?.first
        _temAgenda.value = Agenda.temPermissao(contexto)
        if (_temAgenda.value) viewModelScope.launch {
            _eventos.value = withContext(Dispatchers.IO) { Agenda.proximos(contexto) }
        }
    }

    fun registrarAbertura(app: AppInfo) {
        uso.registrar(app, org.wolfos.launcher.dados.verboDe(app, _escolhas.value))
        _usoMudou.value++
    }

    fun definirRitmo(valor: Boolean) {
        preferencias.ritmo = valor
        _ritmo.value = valor
    }

    fun definirModoNoite(valor: Boolean) {
        preferencias.modoNoite = valor
        _modoNoite.value = valor
    }

    fun definirJanelaNoite(inicio: Int, fim: Int) {
        preferencias.noiteInicio = inicio
        preferencias.noiteFim = fim
        _janelaNoite.value = inicio to fim
    }

    fun adicionarNaMatilha(id: Long) {
        val atual = _matilha.value.ifEmpty { Matilha.pessoas(_contatos.value, emptyList()).map { it.id } }
        if (id in atual || atual.size >= Matilha.MAXIMO) return
        definirMatilha(atual + id)
    }

    fun tirarDaMatilha(id: Long) {
        val atual = _matilha.value.ifEmpty { Matilha.pessoas(_contatos.value, emptyList()).map { it.id } }
        definirMatilha(atual - id)
    }

    private fun definirMatilha(ids: List<Long>) {
        matilhaArq.gravar(ids)
        _matilha.value = ids
    }

    fun mudarVerbo(app: AppInfo, verbo: Verbo?) {
        escolhasArq.gravar(app.chave, verbo)
        _escolhas.value = escolhasArq.ler()
    }

    /** O launcher voltou para a tela: procura versao nova se faz tempo que nao procura. */
    fun aoVoltar() {
        _ehPadrao.value = Padrao.ehPadrao(getApplication())
        _avisosPermitidos.value = Avisos.permitido(getApplication())
        if (_avisosPermitidos.value) Tocando.iniciar(getApplication())
        // a agenda pode ter mudado (ou a permissao chegado) enquanto estava fora
        carregarContatos()
        carregarDia()
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

    fun definirMostrarMatilha(valor: Boolean) {
        preferencias.mostrarMatilha = valor
        _mostrarMatilha.value = valor
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
