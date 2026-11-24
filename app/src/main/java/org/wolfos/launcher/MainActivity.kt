package org.wolfos.launcher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.Verbo
import org.wolfos.launcher.dados.agrupar
import org.wolfos.launcher.dados.verboDe
import org.wolfos.launcher.ui.AcoesApp
import org.wolfos.launcher.ui.AcoesComando
import org.wolfos.launcher.ui.AcoesInicio
import org.wolfos.launcher.ui.AjustesTela
import org.wolfos.launcher.ui.Aurora
import org.wolfos.launcher.ui.Comando
import org.wolfos.launcher.ui.Gaveta
import org.wolfos.launcher.ui.Inicio
import org.wolfos.launcher.ui.VerboTela
import org.wolfos.launcher.ui.WolfTema

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    /** Sobe a cada toque no botao Home: a tela volta para o inicio. */
    private var home by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WolfTema { Tela(vm, home) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        home++
    }

    override fun onResume() {
        super.onResume()
        vm.aoVoltar()
    }
}

/** As telas do launcher. O verbo aberto vai junto no nome ("verbo:OUVIR"). */
private object Telas {
    const val INICIO = "inicio"
    const val GAVETA = "gaveta"
    const val COMANDO = "comando"
    const val AJUSTES = "ajustes"
    const val VERBO = "verbo:"
}

@Composable
private fun Tela(vm: LauncherViewModel, home: Int) {
    val contexto = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val escolhas by vm.escolhas.collectAsStateWithLifecycle()
    val atualizacao by vm.atualizacao.collectAsStateWithLifecycle()
    val instalarSozinho by vm.instalarSozinho.collectAsStateWithLifecycle()
    val contatos by vm.contatos.collectAsStateWithLifecycle()
    val temContatos by vm.temContatos.collectAsStateWithLifecycle()
    val ehPadrao by vm.ehPadrao.collectAsStateWithLifecycle()

    var tela by rememberSaveable { mutableStateOf(Telas.INICIO) }
    var busca by rememberSaveable { mutableStateOf("") }
    var comando by rememberSaveable { mutableStateOf("") }

    val voltarAoInicio = {
        tela = Telas.INICIO
        busca = ""
        comando = ""
    }
    // Home: volta sempre para o inicio
    LaunchedEffect(home) { voltarAoInicio() }
    // voltar volta para o inicio; no inicio nao faz nada (o launcher nao "sai")
    BackHandler(enabled = tela != Telas.INICIO) { voltarAoInicio() }
    BackHandler(enabled = tela == Telas.INICIO) {}

    // ser a tela inicial: o pedido do proprio Android; se ele nao mostrar, a tela de ajuste
    val pedidoPadrao = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.aoVoltar()
    }
    val definirPadrao = {
        val pedido = Padrao.pedido(contexto)
        if (pedido == null || runCatching { pedidoPadrao.launch(pedido) }.isFailure) {
            Apps.abrirTela(contexto, Padrao.telaDeAjuste())
        }
    }
    // na primeira vez que abre, pede sozinho (depois, so pela faixa na tela inicial)
    LaunchedEffect(Unit) {
        if (!vm.preferencias.jaPediuPadrao && !Padrao.ehPadrao(contexto)) {
            vm.preferencias.jaPediuPadrao = true
            definirPadrao()
        }
    }
    val pedidoContatos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.carregarContatos()
    }

    val verbos = remember(apps, escolhas) { agrupar(apps, escolhas) }
    val acoesApp = AcoesApp(
        abrir = { app -> if (Apps.abrir(contexto, app)) voltarAoInicio() },
        verboDe = { verboDe(it, escolhas) },
        mudarVerbo = vm::mudarVerbo,
        informacoes = { Apps.informacoes(contexto, it) },
        desinstalar = { Apps.desinstalar(contexto, it) },
    )
    val acoesInicio = AcoesInicio(
        abrirVerbo = { tela = Telas.VERBO + it.name },
        abrirComando = { texto -> comando = texto; tela = Telas.COMANDO },
        abrirGaveta = { tela = Telas.GAVETA },
        abrirNotificacoes = { Apps.abrirNotificacoes(contexto) },
        abrirAjustes = { tela = Telas.AJUSTES },
        papelDeParede = { Apps.papelDeParede(contexto) },
        atualizar = vm::acaoAtualizacao,
        definirPadrao = definirPadrao,
        abrirApp = acoesApp.abrir,
    )
    val acoesComando = AcoesComando(
        ligar = { if (Contatos.ligar(contexto, it)) voltarAoInicio() },
        mensagem = { if (Contatos.mensagem(contexto, it)) voltarAoInicio() },
        executar = { acao -> acao.executar(contexto); voltarAoInicio() },
        ajustes = { comando = ""; tela = Telas.AJUSTES },
        web = { Apps.buscarNaWeb(contexto, it); voltarAoInicio() },
        pedirContatos = { pedidoContatos.launch(Manifest.permission.READ_CONTACTS) },
        fechar = voltarAoInicio,
    )

    // fora do inicio o wallpaper escurece mais (o texto fica legivel sobre qualquer fundo)
    val escuro by animateFloatAsState(if (tela == Telas.INICIO) 0.45f else 0.88f, label = "escuro")

    Box(Modifier.fillMaxSize()) {
        Aurora(MaterialTheme.colorScheme.primary, escuro)
        AnimatedContent(
            targetState = tela,
            transitionSpec = {
                if (targetState == Telas.INICIO) fadeIn() togetherWith slideOutVertically { it / 4 } + fadeOut()
                else slideInVertically { it / 4 } + fadeIn() togetherWith fadeOut()
            },
            label = "tela",
        ) { atual ->
            when {
                atual == Telas.INICIO -> Inicio(apps, verbos, atualizacao, ehPadrao, acoesInicio)
                atual == Telas.GAVETA -> Gaveta(apps, acoesApp, busca) { busca = it }
                atual == Telas.COMANDO -> Comando(
                    apps = apps,
                    contatos = contatos,
                    temContatos = temContatos,
                    texto = comando,
                    aoMudar = { comando = it },
                    acoesApp = acoesApp,
                    acoes = acoesComando,
                )
                atual == Telas.AJUSTES -> AjustesTela(
                    atualizacao = atualizacao,
                    instalarSozinho = instalarSozinho,
                    aoInstalarSozinho = vm::definirInstalarSozinho,
                    aoAtualizar = vm::acaoAtualizacao,
                    aoProcurar = { pronto -> vm.procurarAtualizacao(pronto) },
                    aoPapelDeParede = { Apps.papelDeParede(contexto) },
                    aoFechar = voltarAoInicio,
                )
                atual.startsWith(Telas.VERBO) -> {
                    val verbo = Verbo.entries.firstOrNull { it.name == atual.removePrefix(Telas.VERBO) }
                    val lista = verbo?.let { verbos[it] }.orEmpty()
                    if (verbo != null) VerboTela(verbo, lista, acoesApp, voltarAoInicio)
                }
                else -> Unit
            }
        }
    }
}
