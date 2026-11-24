package org.wolfos.launcher

import android.Manifest
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.avisos.Avisos
import org.wolfos.launcher.avisos.Tocando
import org.wolfos.launcher.dados.Agenda
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.bloqueio.Bloqueio
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Widgets
import org.wolfos.launcher.dados.Atalhos
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Conversas
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Matilha
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.Ritmo
import org.wolfos.launcher.dados.Uso
import org.wolfos.launcher.dados.Verbo
import org.wolfos.launcher.dados.agrupar
import org.wolfos.launcher.dados.verboDe
import org.wolfos.launcher.ui.AcoesAjustes
import org.wolfos.launcher.ui.AcoesApp
import org.wolfos.launcher.ui.AcoesCompromissos
import org.wolfos.launcher.ui.AcoesPlayer
import org.wolfos.launcher.ui.CompromissosDoDia
import org.wolfos.launcher.ui.TempoAgora
import org.wolfos.launcher.ui.AcoesVerbo
import org.wolfos.launcher.ui.Player
import org.wolfos.launcher.ui.AcoesWidgets
import org.wolfos.launcher.ui.WidgetsTela
import org.wolfos.launcher.ui.EditarVerbo
import org.wolfos.launcher.ui.EstadoAjustes
import org.wolfos.launcher.ui.PaginasAjustes
import org.wolfos.launcher.ui.AcoesAviso
import org.wolfos.launcher.ui.AcoesDia
import org.wolfos.launcher.ui.AcoesMusica
import org.wolfos.launcher.ui.DiaTela
import org.wolfos.launcher.ui.FaixaInfo
import org.wolfos.launcher.ui.Passado
import org.wolfos.launcher.ui.agora
import org.wolfos.launcher.ui.quandoFalta
import org.wolfos.launcher.ui.AcoesPessoa
import org.wolfos.launcher.ui.FileiraMatilha
import org.wolfos.launcher.ui.Previa
import org.wolfos.launcher.ui.AcoesComando
import org.wolfos.launcher.ui.AcoesConversa
import org.wolfos.launcher.ui.FileiraConversas
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
            // o modo noite troca a cor de destaque, entao decide antes do tema
            val modoNoite by vm.modoNoite.collectAsStateWithLifecycle()
            val janela by vm.janelaNoite.collectAsStateWithLifecycle()
            val momento = agora()
            val noite = modoNoite && Ritmo.ehNoite(momento.hour * 60 + momento.minute, janela.first, janela.second)
            WolfTema(noite) { Tela(vm, home, noite, momento.hour) }
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

    // os widgets so se atualizam enquanto o launcher esta visivel (poupa bateria)
    override fun onStart() {
        super.onStart()
        runCatching { Widgets.anfitriao(this).startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { Widgets.anfitriao(this).stopListening() }
    }

    /** O que fazer quando a tela de configurar um widget fechar (ok ou cancelou). */
    private var aoConfigurar: ((Boolean) -> Unit)? = null

    /** Abre a tela de configurar do widget (se ele tiver); o retorno chega em [aoConfigurar]. */
    fun configurarWidget(id: Int, depois: (Boolean) -> Unit) {
        aoConfigurar = depois
        val deu = runCatching {
            Widgets.anfitriao(this).startAppWidgetConfigureActivityForResult(this, id, 0, PEDIDO_CONFIGURAR, null)
        }.isSuccess
        // sem como abrir a configuracao: fica com o widget como esta
        if (!deu) {
            aoConfigurar = null
            depois(true)
        }
    }

    @Deprecated("O AppWidgetHost so entrega o resultado por aqui")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PEDIDO_CONFIGURAR) {
            aoConfigurar?.invoke(resultCode == RESULT_OK)
            aoConfigurar = null
        } else {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private companion object {
        const val PEDIDO_CONFIGURAR = 7001
    }
}

/** As telas do launcher. O verbo aberto vai junto no nome ("verbo:OUVIR"). */
private object Telas {
    const val INICIO = "inicio"
    const val GAVETA = "gaveta"
    const val COMANDO = "comando"
    const val AJUSTES = "ajustes"
    const val DIA = "dia"
    const val WIDGETS = "widgets"
    const val VERBO = "verbo:"
}

@Composable
private fun Tela(vm: LauncherViewModel, home: Int, noite: Boolean, hora: Int) {
    val contexto = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val escolhas by vm.escolhas.collectAsStateWithLifecycle()
    val atualizacao by vm.atualizacao.collectAsStateWithLifecycle()
    val instalarSozinho by vm.instalarSozinho.collectAsStateWithLifecycle()
    val contatos by vm.contatos.collectAsStateWithLifecycle()
    val temContatos by vm.temContatos.collectAsStateWithLifecycle()
    val ehPadrao by vm.ehPadrao.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val avisosPermitidos by vm.avisosPermitidos.collectAsStateWithLifecycle()
    val matilhaIds by vm.matilha.collectAsStateWithLifecycle()
    val mostrarMatilha by vm.mostrarMatilha.collectAsStateWithLifecycle()
    val conversas by vm.conversas.collectAsStateWithLifecycle()
    val fixadas by vm.fixadas.collectAsStateWithLifecycle()
    val tipoMatilha by vm.tipoMatilha.collectAsStateWithLifecycle()
    val ritmo by vm.ritmo.collectAsStateWithLifecycle()
    val modoNoite by vm.modoNoite.collectAsStateWithLifecycle()
    val janelaNoite by vm.janelaNoite.collectAsStateWithLifecycle()
    val fone by vm.fone.collectAsStateWithLifecycle()
    val eventos by vm.eventos.collectAsStateWithLifecycle()
    val temAgenda by vm.temAgenda.collectAsStateWithLifecycle()
    val alarme by vm.alarme.collectAsStateWithLifecycle()
    val usoMudou by vm.usoMudou.collectAsStateWithLifecycle()
    val musica by vm.musica.collectAsStateWithLifecycle()
    val iconesNosVerbos by vm.iconesNosVerbos.collectAsStateWithLifecycle()
    val todosApps by vm.todosApps.collectAsStateWithLifecycle()
    val escondidos by vm.escondidos.collectAsStateWithLifecycle()
    val verbosEscondidos by vm.verbosEscondidos.collectAsStateWithLifecycle()
    val titulos by vm.titulos.collectAsStateWithLifecycle()
    val widgets by vm.widgets.collectAsStateWithLifecycle()
    val mostrarPlayer by vm.mostrarPlayer.collectAsStateWithLifecycle()
    val detalheMusica by vm.detalheMusica.collectAsStateWithLifecycle()
    val clima by vm.clima.collectAsStateWithLifecycle()
    val previsao by vm.previsao.collectAsStateWithLifecycle()
    val tempoLigado by vm.tempoLigado.collectAsStateWithLifecycle()
    val cidade by vm.cidade.collectAsStateWithLifecycle()
    val fahrenheit by vm.fahrenheit.collectAsStateWithLifecycle()
    val mostrarCompromissos by vm.compromissos.collectAsStateWithLifecycle()
    val fundoAnimado by vm.fundoAnimado.collectAsStateWithLifecycle()
    // o player aparece tocando, e ate 15 min depois de pausar (a hora muda a cada minuto e reavalia)
    val musicaNoPlayer = remember(musica, mostrarPlayer, hora, agora().minute) {
        musica?.takeIf { m ->
            mostrarPlayer && (m.tocando || android.os.SystemClock.elapsedRealtime() - m.atualizadoEm < 15 * 60_000L)
        }
    }
    val doisToques by vm.doisToques.collectAsStateWithLifecycle()
    val bloqueioLigado by vm.bloqueioLigado.collectAsStateWithLifecycle()
    // o verbo com o menu de editar aberto (vale para a tela inicial e para os ajustes)
    var verboEditando by rememberSaveable { mutableStateOf<String?>(null) }
    var paginaAjustes by rememberSaveable { mutableStateOf(PaginasAjustes.PRINCIPAL) }

    var tela by rememberSaveable { mutableStateOf(Telas.INICIO) }
    var busca by rememberSaveable { mutableStateOf("") }
    var comando by rememberSaveable { mutableStateOf("") }

    val voltarAoInicio = {
        tela = Telas.INICIO
        busca = ""
        comando = ""
    }
    // Home: volta sempre para o inicio
    LaunchedEffect(home) {
        voltarAoInicio()
        verboEditando = null
    }
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

    // escolher alguem da agenda para a Matilha (o seletor do Android nao precisa de permissao,
    // mas o numero vem da nossa leitura da agenda, que precisa)
    val escolherPessoa = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.lastPathSegment?.toLongOrNull()?.let(vm::adicionarNaMatilha)
    }

    val pedidoAgenda = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.carregarDia()
    }

    // todos os verbos com os apps (para os ajustes) e os que aparecem na tela inicial
    val verbosCompletos = remember(apps, escolhas) { agrupar(apps, escolhas) }
    // adicionar widget: pedir um id, ligar (perguntando na primeira vez), configurar, guardar
    var widgetPendente by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }
    val guardarOuConfigurar = { id: Int, info: AppWidgetProviderInfo ->
        val altura = Widgets.alturaInicial(contexto, info)
        if (info.configure != null) {
            (contexto as? MainActivity)?.configurarWidget(id) { ok -> if (ok) vm.adicionarWidget(id, altura) else Widgets.apagar(contexto, id) }
                ?: vm.adicionarWidget(id, altura)
        } else {
            vm.adicionarWidget(id, altura)
        }
    }
    val pedidoWidget = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        widgetPendente?.let { (id, info) ->
            if (r.resultCode == android.app.Activity.RESULT_OK) guardarOuConfigurar(id, info) else Widgets.apagar(contexto, id)
        }
        widgetPendente = null
    }
    val adicionarWidget = { info: AppWidgetProviderInfo ->
        val id = Widgets.novoId(contexto)
        if (Widgets.ligar(contexto, id, info)) {
            guardarOuConfigurar(id, info)
        } else {
            widgetPendente = id to info
            pedidoWidget.launch(Widgets.pedidoParaLigar(id, info))
        }
    }

    val todosOsVerbos = remember(verbosCompletos, verbosEscondidos) { verbosCompletos.filterKeys { it !in verbosEscondidos } }
    // a ordem do momento: hora do dia, costume da pessoa, fone, modo noite
    val pontos = remember(hora, usoMudou) { vm.uso.pontosDosVerbos(hora) }
    // com musica tocando o Ouvir sobe, como com o fone
    val tocando = musica?.tocando == true
    val verbos = remember(todosOsVerbos, hora, pontos, fone, tocando, noite, ritmo) {
        Ritmo.ordenar(todosOsVerbos, hora, pontos, fone || tocando, noite, ritmo)
    }
    val iconesDosVerbos = remember(verbos, hora, usoMudou, iconesNosVerbos) {
        if (iconesNosVerbos) vm.uso.destaques(verbos, hora, 3) else emptyMap()
    }
    val previas = remember(avisos, apps, escolhas) { previasDosVerbos(avisos, apps, escolhas) }
    val pessoas = remember(contatos, matilhaIds) { Matilha.pessoas(contatos, matilhaIds) }
    val acoesApp = AcoesApp(
        abrir = { app ->
            if (Apps.abrir(contexto, app)) {
                vm.registrarAbertura(app)
                voltarAoInicio()
            }
        },
        verboDe = { verboDe(it, escolhas) },
        mudarVerbo = vm::mudarVerbo,
        informacoes = { Apps.informacoes(contexto, it) },
        desinstalar = { Apps.desinstalar(contexto, it) },
        renomear = vm::renomear,
        esconder = { vm.esconder(it) },
        abrirAtalho = { if (Atalhos.abrir(contexto, it)) voltarAoInicio() },
        tituloDoVerbo = { titulos[it] ?: it.titulo },
    )
    val acoesInicio = AcoesInicio(
        abrirVerbo = { tela = Telas.VERBO + it.name },
        abrirComando = { texto -> comando = texto; tela = Telas.COMANDO },
        abrirGaveta = { tela = Telas.GAVETA },
        abrirNotificacoes = { Apps.abrirNotificacoes(contexto) },
        abrirAjustes = { paginaAjustes = PaginasAjustes.PRINCIPAL; tela = Telas.AJUSTES },
        editarVerbo = { verboEditando = it.name },
        ajustesDosVerbos = { paginaAjustes = PaginasAjustes.VERBOS; tela = Telas.AJUSTES },
        abrirWidgets = { tela = Telas.WIDGETS },
        bloquear = if (doisToques) ({ Bloqueio.bloquear() }) else null,
        ligarBloqueio = { Bloqueio.pedirPermissao(contexto) },
        permitirAvisos = if (avisosPermitidos) null else ({ Avisos.pedirPermissao(contexto) }),
        papelDeParede = { Apps.papelDeParede(contexto) },
        atualizar = vm::acaoAtualizacao,
        definirPadrao = definirPadrao,
        abrirApp = acoesApp.abrir,
        abrirDia = { vm.carregarDia(); tela = Telas.DIA },
    )
    val abrirAlarmes = { Apps.abrirTela(contexto, Intent(AlarmClock.ACTION_SHOW_ALARMS)); Unit }
    val acoesMusica = AcoesMusica(
        tocarOuPausar = Tocando::tocarOuPausar,
        proxima = Tocando::proxima,
        abrir = {
            musica?.let { m -> apps.firstOrNull { it.pacote == m.pacote } }?.let(acoesApp.abrir)
        },
    )
    val faixaAgora = remember(noite, alarme, eventos, hora, mostrarCompromissos) {
        faixaDoAgora(
            // com os compromissos na tela, a faixa nao repete o proximo evento
            noite, alarme, if (mostrarCompromissos) emptyList() else eventos, abrirAlarmes,
            criarAlarme = { Apps.abrirTela(contexto, Intent(AlarmClock.ACTION_SET_ALARM)) },
            entrar = { Agenda.entrar(contexto, it) },
            abrir = { Agenda.abrir(contexto, it) },
        )
    }
    val acoesDia = AcoesDia(
        abrirApp = acoesApp.abrir,
        abrirAviso = { a ->
            val app = apps.firstOrNull { it.pacote == a.pacote }
            if (Avisos.abrir(contexto, a) || (app != null && Apps.abrir(contexto, app))) voltarAoInicio()
        },
        abrirEvento = { Agenda.abrir(contexto, it) },
        entrarEvento = { Agenda.entrar(contexto, it) },
        abrirAlarmes = abrirAlarmes,
        permitirAgenda = { pedidoAgenda.launch(Manifest.permission.READ_CALENDAR) },
        buscar = { tela = Telas.COMANDO },
        voltar = voltarAoInicio,
    )
    val acoesAviso = AcoesAviso(
        abrir = { a ->
            val app = apps.firstOrNull { it.pacote == a.pacote }
            if (Avisos.abrir(contexto, a) || (app != null && Apps.abrir(contexto, app))) voltarAoInicio()
        },
        dispensar = Avisos::dispensar,
        permitir = { Avisos.pedirPermissao(contexto) },
        responder = { a, texto -> Avisos.responder(contexto, a, texto) },
        marcarLida = { Avisos.marcarComoLida(it) },
    )
    val acoesPessoa = AcoesPessoa(
        ligar = { Contatos.ligar(contexto, it) },
        mensagem = { Contatos.mensagem(contexto, it) },
        whatsapp = if (remember(apps) { Matilha.temWhatsapp(contexto) }) { c -> Matilha.whatsapp(contexto, c) } else null,
        verNaAgenda = { Matilha.verNaAgenda(contexto, it) },
        abrirAviso = acoesAviso.abrir,
        tirar = { vm.tirarDaMatilha(it.id) },
        adicionar = {
            if (temContatos) runCatching { escolherPessoa.launch(null) }
            else pedidoContatos.launch(Manifest.permission.READ_CONTACTS)
        },
    )
    val acoesConversa = AcoesConversa(
        abrir = { c ->
            // se o atalho falhar (conversa apagada), abre o WhatsApp
            if (Conversas.abrir(contexto, c) || apps.firstOrNull { it.pacote == c.pacote }?.let { Apps.abrir(contexto, it) } == true) {
                voltarAoInicio()
            }
        },
        alternarFixada = vm::alternarFixada,
        abrirAviso = acoesAviso.abrir,
    )
    // conversas que vem so com o numero ganham o nome da agenda
    val conversasComNome = remember(conversas, contatos) { Conversas.comNomes(conversas, contatos) }
    val daMatilha = remember(conversasComNome, fixadas, tipoMatilha, avisos) {
        Conversas.matilha(conversasComNome, fixadas, tipoMatilha, avisos)
    }
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
        Aurora(MaterialTheme.colorScheme.primary, escuro, degrade = tela == Telas.INICIO, animado = fundoAnimado)
        AnimatedContent(
            targetState = tela,
            transitionSpec = {
                when {
                    targetState == Telas.DIA -> slideInHorizontally { -it / 3 } + fadeIn() togetherWith fadeOut()
                    targetState == Telas.WIDGETS -> slideInHorizontally { it / 3 } + fadeIn() togetherWith fadeOut()
                    initialState == Telas.WIDGETS -> fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
                    initialState == Telas.DIA -> fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                    targetState == Telas.INICIO ->
                        (fadeIn(tween(350)) + scaleIn(tween(400), initialScale = 0.94f)) togetherWith
                            (slideOutVertically { it / 4 } + fadeOut(tween(250)))
                    else -> slideInVertically { it / 4 } + fadeIn() togetherWith fadeOut()
                }
            },
            label = "tela",
        ) { atual ->
            when {
                atual == Telas.INICIO -> Inicio(
                    apps, verbos, atualizacao, ehPadrao, previas, faixaAgora, noite,
                    iconesDosVerbos, titulos, musica, acoesMusica, acoesInicio,
                    clima = clima,
                    tempo = previsao?.takeIf { tempoLigado }?.let { p ->
                        { TempoAgora(p, if (fahrenheit) "°F" else "°", aoTocar = { vm.atualizarTempo(forcar = true) }) }
                    },
                    compromissos = if (mostrarCompromissos) {
                        {
                            CompromissosDoDia(
                                eventos, temAgenda,
                                AcoesCompromissos(
                                    abrir = { Agenda.abrir(contexto, it) },
                                    entrar = { Agenda.entrar(contexto, it) },
                                    novo = { Apps.abrirTela(contexto, Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)) },
                                    permitir = { pedidoAgenda.launch(Manifest.permission.READ_CALENDAR) },
                                ),
                            )
                        }
                    } else null,
                    player = musicaNoPlayer?.let { m ->
                        {
                            Player(
                                m,
                                AcoesPlayer(
                                    tocarOuPausar = Tocando::tocarOuPausar,
                                    anterior = Tocando::anterior,
                                    proxima = Tocando::proxima,
                                    irPara = Tocando::irPara,
                                    abrir = acoesMusica.abrir,
                                ),
                            )
                        }
                    },
                ) {
                    when {
                        !mostrarMatilha -> Unit
                        // com o WhatsApp: as conversas recentes (pessoas e grupos) e as fixadas
                        conversas.isNotEmpty() -> FileiraConversas(daMatilha, conversasComNome, fixadas, avisos, acoesConversa)
                        // sem ele: as pessoas da agenda
                        else -> FileiraMatilha(pessoas, avisos, temContatos, acoesPessoa)
                    }
                }
                atual == Telas.DIA -> {
                    val porChave = remember(apps) { apps.associateBy { it.chave } }
                    val porPacote = remember(apps) { apps.associateBy { it.pacote } }
                    val inicioDoDia = Uso.inicioDoDia()
                    val antes = remember(usoMudou, avisos, apps) {
                        (vm.uso.hoje().mapNotNull { a -> porChave[a.chave]?.let { Passado.Abriu(a.quando, it) } } +
                            avisos.filter { it.quando >= inicioDoDia }.map { Passado.Recebeu(it.quando, it, porPacote[it.pacote]) })
                            .sortedBy { it.quando }
                            .takeLast(8)
                    }
                    val costume = remember(hora, usoMudou, apps) {
                        vm.uso.appsDaHora(hora, apps, 5).ifEmpty {
                            vm.uso.hoje().asReversed().mapNotNull { porChave[it.chave] }.distinct().take(5)
                        }
                    }
                    DiaTela(antes, costume, eventos, alarme, temAgenda, acoesDia)
                }
                atual == Telas.WIDGETS -> WidgetsTela(
                    widgets,
                    AcoesWidgets(
                        adicionar = adicionarWidget,
                        remover = vm::removerWidget,
                        mudarAltura = vm::mudarAlturaWidget,
                        voltar = voltarAoInicio,
                    ),
                )
                atual == Telas.GAVETA -> {
                    val porChave = remember(apps) { apps.associateBy { it.chave } }
                    val recentes = remember(usoMudou, apps) {
                        vm.uso.hoje().asReversed().mapNotNull { porChave[it.chave] }.distinct().take(5)
                    }
                    val contagem = remember(avisos) { avisos.groupingBy { it.pacote }.eachCount() }
                    Gaveta(apps, acoesApp, busca, { busca = it }, recentes, contagem)
                }
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
                    EstadoAjustes(
                        atualizacao = atualizacao,
                        instalarSozinho = instalarSozinho,
                        ehPadrao = ehPadrao,
                        avisosPermitidos = avisosPermitidos,
                        temAgenda = temAgenda,
                        temContatos = temContatos,
                        mostrarMatilha = mostrarMatilha,
                        tipoMatilha = tipoMatilha,
                        matilhaPeloWhatsapp = conversas.isNotEmpty(),
                        iconesNosVerbos = iconesNosVerbos,
                        mostrarPlayer = mostrarPlayer,
                        detalhePlayer = detalheMusica,
                        tempoLigado = tempoLigado,
                        cidade = cidade?.descricao,
                        fahrenheit = fahrenheit,
                        compromissos = mostrarCompromissos,
                        fundoAnimado = fundoAnimado,
                        ritmo = ritmo,
                        modoNoite = modoNoite,
                        janelaNoite = janelaNoite,
                        doisToques = doisToques,
                        bloqueioLigado = bloqueioLigado,
                        appsPorVerbo = verbosCompletos.mapValues { it.value.size },
                        titulos = titulos,
                        verbosEscondidos = verbosEscondidos,
                        appsEscondidos = todosApps.filter { it.chave in escondidos },
                    ),
                    AcoesAjustes(
                        fechar = voltarAoInicio,
                        atualizar = vm::acaoAtualizacao,
                        procurar = { pronto -> vm.procurarAtualizacao(pronto) },
                        instalarSozinho = vm::definirInstalarSozinho,
                        definirPadrao = definirPadrao,
                        permitirAvisos = { Avisos.pedirPermissao(contexto) },
                        permitirAgenda = { pedidoAgenda.launch(Manifest.permission.READ_CALENDAR) },
                        permitirContatos = { pedidoContatos.launch(Manifest.permission.READ_CONTACTS) },
                        informacoesDoLauncher = {
                            Apps.abrirTela(
                                contexto,
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + contexto.packageName)),
                            )
                        },
                        papelDeParede = { Apps.papelDeParede(contexto) },
                        mostrarMatilha = vm::definirMostrarMatilha,
                        tipoMatilha = vm::definirTipoMatilha,
                        iconesNosVerbos = vm::definirIconesNosVerbos,
                        mostrarPlayer = vm::definirMostrarPlayer,
                        tempoLigado = vm::definirTempoLigado,
                        escolherCidade = vm::definirCidade,
                        fahrenheit = vm::definirFahrenheit,
                        compromissos = vm::definirCompromissos,
                        fundoAnimado = vm::definirFundoAnimado,
                        ritmo = vm::definirRitmo,
                        modoNoite = vm::definirModoNoite,
                        janelaNoite = vm::definirJanelaNoite,
                        editarVerbo = { verboEditando = it.name },
                        doisToques = vm::definirDoisToques,
                        ligarBloqueio = { Bloqueio.pedirPermissao(contexto) },
                        esconderVerbo = vm::esconderVerbo,
                        mostrarApp = { vm.esconder(it, false) },
                        abrirCodigo = { Apps.abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TioDanoni/wolf-launcher"))) },
                    ),
                    paginaInicial = paginaAjustes,
                )
                atual.startsWith(Telas.VERBO) -> {
                    val verbo = Verbo.entries.firstOrNull { it.name == atual.removePrefix(Telas.VERBO) }
                    val lista = verbo?.let { verbos[it] }.orEmpty()
                    if (verbo != null) VerboTela(verbo, titulos[verbo] ?: verbo.titulo, lista, avisos, avisosPermitidos, acoesApp, acoesAviso, voltarAoInicio)
                }
                else -> Unit
            }
        }

        // o menu de editar um verbo (segurar na tela inicial, ou tocar nos ajustes)
        verboEditando?.let { nome -> Verbo.entries.firstOrNull { it.name == nome } }?.let { v ->
            EditarVerbo(
                verbo = v,
                titulo = titulos[v] ?: v.titulo,
                apps = verbosCompletos[v].orEmpty(),
                todos = apps,
                escondido = v in verbosEscondidos,
                acoes = AcoesVerbo(
                    renomear = vm::renomearVerbo,
                    esconder = vm::esconderVerbo,
                    mudarVerbo = vm::mudarVerbo,
                ),
                aoFechar = { verboEditando = null },
            )
        }
    }
}

/** Para cada verbo, a notificacao mais nova dos apps dele e quantas tem. */
private fun previasDosVerbos(avisos: List<Aviso>, apps: List<AppInfo>, escolhas: Map<String, String>): Map<Verbo, Previa> {
    if (avisos.isEmpty()) return emptyMap()
    val verboDoPacote = HashMap<String, Verbo>()
    for (app in apps) verboDe(app, escolhas)?.let { verboDoPacote.putIfAbsent(app.pacote, it) }
    return avisos
        .groupBy { verboDoPacote[it.pacote] }
        .mapNotNull { (verbo, lista) ->
            if (verbo == null) return@mapNotNull null
            val a = lista.first()
            val texto = if (a.titulo.isNotEmpty() && a.texto.isNotEmpty()) "${a.titulo}: ${a.texto}" else a.titulo + a.texto
            verbo to Previa(texto, lista.size)
        }
        .toMap()
}

/**
 * A faixa do agora na tela inicial: a noite, o despertador; de dia, o proximo
 * compromisso das proximas 3 horas (com "Entrar" se tiver link de reuniao).
 */
private fun faixaDoAgora(
    noite: Boolean,
    alarme: Long?,
    eventos: List<Evento>,
    abrirAlarmes: () -> Unit,
    criarAlarme: () -> Unit,
    entrar: (Evento) -> Unit,
    abrir: (Evento) -> Unit,
): FaixaInfo? {
    val agora = System.currentTimeMillis()
    if (noite && alarme != null) {
        val sono = ((alarme - agora) / 60_000).coerceAtLeast(0)
        val hora = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(alarme), java.time.ZoneId.systemDefault())
        val texto = "Despertador %02d:%02d · %d h %02d de sono".format(hora.hour, hora.minute, sono / 60, sono % 60)
        return FaixaInfo("⏰", texto, "Ver", abrirAlarmes)
    }
    if (noite) return FaixaInfo("⏰", "Nenhum despertador marcado", "Criar", criarAlarme)
    val e = eventos.firstOrNull { !it.diaInteiro && it.inicio - agora < 3 * 3_600_000L } ?: return null
    val quando = if (e.inicio <= agora) "agora" else quandoFalta(e.inicio, agora)
    return if (e.link != null) FaixaInfo("◷", "${e.titulo} · $quando", "Entrar") { entrar(e) }
    else FaixaInfo("◷", "${e.titulo} · $quando", "Ver") { abrir(e) }
}
