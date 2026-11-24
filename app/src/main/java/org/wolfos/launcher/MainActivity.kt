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
import org.wolfos.launcher.dados.AcaoGesto
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Gesto
import org.wolfos.launcher.dados.Widgets
import org.wolfos.launcher.dados.Atalhos
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Conversas
import org.wolfos.launcher.dados.FonteMatilha
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Matilha
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.Ritmo
import org.wolfos.launcher.dados.Uso
import org.wolfos.launcher.ui.AcoesAjustes
import org.wolfos.launcher.ui.AcoesApp
import org.wolfos.launcher.ui.AcoesCompromissos
import org.wolfos.launcher.ui.AcoesPlayer
import org.wolfos.launcher.ui.CompromissosDoDia
import org.wolfos.launcher.ui.PrevisaoSemana
import org.wolfos.launcher.ui.TempoAgora
import org.wolfos.launcher.ui.eventosDeHoje
import org.wolfos.launcher.ui.Player
import org.wolfos.launcher.ui.AcoesWidgets
import org.wolfos.launcher.ui.WidgetsTela
import org.wolfos.launcher.ui.EstadoAjustes
import org.wolfos.launcher.ui.PaginasAjustes
import org.wolfos.launcher.ui.AcoesAviso
import org.wolfos.launcher.ui.AcoesDia
import org.wolfos.launcher.ui.AcoesBusca
import org.wolfos.launcher.ui.AcoesPainel
import org.wolfos.launcher.ui.EstadoPainel
import org.wolfos.launcher.ui.PainelTela
import org.wolfos.launcher.ui.AcoesMusica
import org.wolfos.launcher.ui.BuscaTela
import org.wolfos.launcher.ui.ListaInicio
import org.wolfos.launcher.dados.Favoritos
import org.wolfos.launcher.dados.Sugestoes
import org.wolfos.launcher.ui.DiaTela
import org.wolfos.launcher.ui.FaixaInfo
import org.wolfos.launcher.ui.Passado
import org.wolfos.launcher.ui.agora
import org.wolfos.launcher.ui.quandoFalta
import org.wolfos.launcher.ui.AcoesPessoa
import org.wolfos.launcher.ui.FileiraMatilha
import org.wolfos.launcher.ui.AcoesComando
import org.wolfos.launcher.ui.AcoesConversa
import org.wolfos.launcher.ui.FileiraConversas
import org.wolfos.launcher.ui.AcoesInicio
import org.wolfos.launcher.ui.AjustesTela
import org.wolfos.launcher.ui.Aurora
import org.wolfos.launcher.ui.Comando
import org.wolfos.launcher.ui.Gaveta
import org.wolfos.launcher.ui.Inicio
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
            val paleta by vm.paleta.collectAsStateWithLifecycle()
            val coresWallpaper by vm.coresWallpaper.collectAsStateWithLifecycle()
            val momento = agora()
            val noite = modoNoite && Ritmo.ehNoite(momento.hour * 60 + momento.minute, janela.first, janela.second)
            WolfTema(noite, paleta, coresWallpaper) { Tela(vm, home, noite, momento.hour) }
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

/** As telas do launcher. */
private object Telas {
    const val INICIO = "inicio"
    const val GAVETA = "gaveta"
    const val COMANDO = "comando"
    const val AJUSTES = "ajustes"
    const val DIA = "dia"
    const val WIDGETS = "widgets"
}

@Composable
private fun Tela(vm: LauncherViewModel, home: Int, noite: Boolean, hora: Int) {
    val contexto = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favoritosChaves by vm.favoritos.collectAsStateWithLifecycle()
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
    val todosApps by vm.todosApps.collectAsStateWithLifecycle()
    val escondidos by vm.escondidos.collectAsStateWithLifecycle()
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
    val estiloRelogio by vm.estiloRelogio.collectAsStateWithLifecycle()
    // o player aparece tocando, e ate 15 min depois de pausar (a hora muda a cada minuto e reavalia)
    val musicaNoPlayer = remember(musica, mostrarPlayer, hora, agora().minute) {
        musica?.takeIf { m ->
            mostrarPlayer && (m.tocando || android.os.SystemClock.elapsedRealtime() - m.atualizadoEm < 15 * 60_000L)
        }
    }
    val gestos by vm.gestos.collectAsStateWithLifecycle()
    val matilhaSoFixadas by vm.matilhaSoFixadas.collectAsStateWithLifecycle()
    val mostrarMarca by vm.mostrarMarca.collectAsStateWithLifecycle()
    val escuroInicio by vm.escuroInicio.collectAsStateWithLifecycle()
    val bloqueioLigado by vm.bloqueioLigado.collectAsStateWithLifecycle()
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

    // os fones e aparelhos Bluetooth do Painel (Android 12+ pede a permissao)
    val temBluetooth = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
        androidx.core.content.ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val pedidoBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.carregarAparelhos() }
    val pedidoAgenda = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.carregarDia()
    }

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

    // o painel: favoritos (os escolhidos, ou os mais usados) e o "Agora" (o que faz sentido ja)
    val notasUso = remember(hora, usoMudou) { vm.uso.notas(hora) }
    val porChaveApp = remember(apps) { apps.associateBy { it.chave } }
    val favoritosApps = remember(favoritosChaves, porChaveApp, notasUso) {
        favoritosChaves.mapNotNull { porChaveApp[it] }.ifEmpty { Sugestoes.favoritosPadrao(apps, notasUso) }
    }
    val avisosPorPacote = remember(avisos) { avisos.groupingBy { it.pacote }.eachCount() }
    // a lista da tela inicial, conforme o ajuste: so os fixados, os mais usados, ou os fixados
    // completados (ate 9) com o que faz sentido agora
    val modoLista by vm.modoLista.collectAsStateWithLifecycle()
    val (listaFixa, agoraApps) = remember(modoLista, favoritosChaves, porChaveApp, favoritosApps, notasUso, avisosPorPacote, musica?.pacote, usoMudou) {
        when (modoLista) {
            "FIXADOS" -> favoritosChaves.mapNotNull { porChaveApp[it] } to emptyList()
            "MAIS_USADOS" -> {
                val totais = vm.uso.totais()
                val usados = apps.filter { (totais[it.chave] ?: 0) > 0 }.sortedByDescending { totais[it.chave] }.take(9)
                usados.ifEmpty { Sugestoes.favoritosPadrao(apps, notasUso, 9) } to emptyList()
            }
            else -> favoritosApps to Sugestoes.agora(
                apps, notasUso, avisosPorPacote, musica?.pacote?.takeIf { musica?.tocando == true },
                favoritosApps.map { it.chave }.toSet(), quantos = (9 - favoritosApps.size).coerceAtLeast(0),
            )
        }
    }
    // a folha da previsao da semana (tocar no clima)
    var mostrandoSemana by remember { mutableStateOf(false) }
    // a ultima mensagem de cada app ("Ana: chego em 10 min") e o resumo das notificacoes
    val ultimas = remember(avisos) {
        avisos.groupBy { it.pacote }.mapValues { (_, l) ->
            val a = l.first()
            if (a.titulo.isNotEmpty() && a.texto.isNotEmpty()) "${a.titulo}: ${a.texto}" else a.titulo + a.texto
        }
    }
    val matilhaOrganizar by vm.matilhaOrganizar.collectAsStateWithLifecycle()
    val pessoas = remember(contatos, matilhaIds, avisos, matilhaOrganizar) {
        val lista = Matilha.pessoas(contatos, matilhaIds)
        if (!matilhaOrganizar) lista
        else lista.partition { Matilha.esperando(it, avisos).isNotEmpty() }.let { (com, sem) ->
            com.sortedByDescending { p -> Matilha.esperando(p, avisos).maxOf { it.quando } } + sem
        }
    }
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
    val acoesApp = AcoesApp(
        abrir = { app ->
            if (Apps.abrir(contexto, app)) {
                vm.registrarAbertura(app)
                voltarAoInicio()
            }
        },
        informacoes = { Apps.informacoes(contexto, it) },
        desinstalar = { Apps.desinstalar(contexto, it) },
        renomear = vm::renomear,
        esconder = { vm.esconder(it) },
        abrirAtalho = { if (Atalhos.abrir(contexto, it)) voltarAoInicio() },
        ehFavorito = { it.chave in favoritosChaves },
        alternarFavorito = { app ->
            // o primeiro fixado vira a lista (os mais usados eram so um comeco)
            vm.alternarFavorito(app)
        },
        avisosDe = { app -> avisos.filter { it.pacote == app.pacote } },
        limparAvisos = { app -> avisos.filter { it.pacote == app.pacote }.forEach(Avisos::dispensar) },
        acoesAviso = acoesAviso,
    )
    val acoesInicio = AcoesInicio(
        // deslizar para cima: o buscador universal, ja com o teclado
        abrirComando = { texto -> comando = texto; tela = Telas.COMANDO },
        abrirGaveta = { comando = ""; tela = Telas.COMANDO },
        abrirNotificacoes = { Apps.abrirNotificacoes(contexto) },
        abrirAjustes = { paginaAjustes = PaginasAjustes.PRINCIPAL; tela = Telas.AJUSTES },
        abrirWidgets = { tela = Telas.WIDGETS },
        ligarBloqueio = { Bloqueio.pedirPermissao(contexto) },
        // cada gesto faz o que a pessoa escolheu (aba Gestos dos ajustes)
        gesto = { g ->
            when (gestos[g] ?: AcaoGesto.NADA) {
                AcaoGesto.BUSCA -> { comando = ""; tela = Telas.COMANDO; true }
                AcaoGesto.TODOS_APPS -> { busca = ""; tela = Telas.GAVETA; true }
                AcaoGesto.NOTIFICACOES -> { Apps.abrirNotificacoes(contexto); true }
                AcaoGesto.DIA -> { vm.carregarDia(); tela = Telas.DIA; true }
                AcaoGesto.WIDGETS -> { tela = Telas.WIDGETS; true }
                AcaoGesto.BLOQUEAR -> Bloqueio.bloquear()
                AcaoGesto.NADA -> true
            }
        },
        doisToquesAtivo = (gestos[Gesto.DOIS_TOQUES] ?: AcaoGesto.NADA) != AcaoGesto.NADA,
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
        buscar = { comando = ""; tela = Telas.COMANDO },
        voltar = voltarAoInicio,
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
                vm.registrarConversa(c)
                voltarAoInicio()
            }
        },
        alternarFixada = vm::alternarFixada,
        abrirAviso = acoesAviso.abrir,
    )
    // conversas que vem so com o numero ganham o nome da agenda
    // so as conversas do WhatsApp escolhido (as fixadas ficam sempre)
    val fonteMatilha by vm.fonteMatilha.collectAsStateWithLifecycle()
    val conversasComNome = remember(conversas, contatos, fonteMatilha, fixadas) {
        Conversas.comNomes(conversas.filter { it.pacote in fonteMatilha.pacotes || it.chave in fixadas }, contatos)
    }
    val usoConversas by vm.usoConversas.collectAsStateWithLifecycle()
    val daMatilha = remember(conversasComNome, fixadas, tipoMatilha, avisos, usoConversas, matilhaSoFixadas, matilhaOrganizar) {
        Conversas.matilha(
            conversasComNome, fixadas, tipoMatilha, avisos, usoConversas,
            soFixadas = matilhaSoFixadas, organizarPorAvisos = matilhaOrganizar,
        )
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
    val escuroDoInicio = when (escuroInicio) {
        "LEVE" -> 0.30f
        "FORTE" -> 0.62f
        else -> 0.45f
    }
    val escuro by animateFloatAsState(if (tela == Telas.INICIO) escuroDoInicio else 0.88f, label = "escuro")

    Box(Modifier.fillMaxSize()) {
        Aurora(
            MaterialTheme.colorScheme.primary, escuro, segunda = MaterialTheme.colorScheme.secondary,
            degrade = tela == Telas.INICIO, animado = fundoAnimado,
        )
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
                    atualizacao, ehPadrao, faixaAgora, noite, acoesInicio,
                    painel = {
                        ListaInicio(
                            favoritos = listaFixa,
                            sugeridos = agoraApps,
                            todos = apps,
                            avisos = avisosPorPacote,
                            ultimas = ultimas,
                            tocando = musica?.pacote?.takeIf { musica?.tocando == true },
                            acoes = acoesApp,
                            reinicio = home,
                        )
                    },
                    clima = clima,
                    mostrarMarca = mostrarMarca,
                    estiloRelogio = estiloRelogio,
                    tempo = previsao?.takeIf { tempoLigado }?.let { p ->
                        { TempoAgora(p, if (fahrenheit) "°F" else "°", aoTocar = { vm.atualizarTempo(forcar = true); mostrandoSemana = true }) }
                    },
                    compromissos = if (mostrarCompromissos && temAgenda && eventosDeHoje(eventos).isNotEmpty()) {
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
                        fonteMatilha != FonteMatilha.AGENDA && conversasComNome.isNotEmpty() -> FileiraConversas(daMatilha, conversasComNome, fixadas, avisos, acoesConversa)
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
                atual == Telas.WIDGETS -> {
                    val notas by vm.notas.collectAsStateWithLifecycle()
                    val aparelhos by vm.aparelhos.collectAsStateWithLifecycle()
                    val timerMudou by vm.timerMudou.collectAsStateWithLifecycle()
                    val ordemPainel by vm.ordemPainel.collectAsStateWithLifecycle()
                    val escondidasPainel by vm.escondidasPainel.collectAsStateWithLifecycle()
                    PainelTela(
                        EstadoPainel(
                            previsao = previsao?.takeIf { tempoLigado },
                            unidade = if (fahrenheit) "°F" else "°",
                            aparelhos = aparelhos,
                            temBluetooth = temBluetooth,
                            timer = vm.timer,
                            timerMudou = timerMudou,
                            musica = musica,
                            notas = notas,
                            eventos = eventos,
                            temAgenda = temAgenda,
                            widgets = widgets,
                            ordem = ordemPainel,
                            escondidas = escondidasPainel,
                        ),
                        AcoesPainel(
                            voltar = voltarAoInicio,
                            abrirClima = { if (previsao != null) mostrandoSemana = true else { paginaAjustes = PaginasAjustes.PRINCIPAL; tela = Telas.AJUSTES } },
                            pedirBluetooth = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) pedidoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                else vm.carregarAparelhos()
                            },
                            scanner = {
                                // a camera abre no modo foto; ela le QR code sozinha na maioria dos celulares
                                Apps.abrirTela(contexto, Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                            },
                            comecarTimer = vm::comecarTimer,
                            pararTimer = vm::pararTimer,
                            alternarCrono = vm::alternarCrono,
                            zerarCrono = vm::zerarCrono,
                            acoesPlayer = AcoesPlayer(
                                tocarOuPausar = Tocando::tocarOuPausar,
                                anterior = Tocando::anterior,
                                proxima = Tocando::proxima,
                                irPara = Tocando::irPara,
                                abrir = acoesMusica.abrir,
                            ),
                            adicionarNota = vm::adicionarNota,
                            alternarNota = vm::alternarNota,
                            apagarNota = vm::apagarNota,
                            limparFeitas = vm::limparFeitas,
                            abrirEvento = { Agenda.abrir(contexto, it) },
                            entrarEvento = { Agenda.entrar(contexto, it) },
                            permitirAgenda = { pedidoAgenda.launch(Manifest.permission.READ_CALENDAR) },
                            acoesWidgets = AcoesWidgets(
                                adicionar = adicionarWidget,
                                remover = vm::removerWidget,
                                mudarAltura = vm::mudarAlturaWidget,
                                voltar = voltarAoInicio,
                            ),
                            moverParte = vm::moverParte,
                            alternarParte = vm::alternarParte,
                        ),
                    )
                }
                atual == Telas.GAVETA -> {
                    val porChave = remember(apps) { apps.associateBy { it.chave } }
                    val recentes = remember(usoMudou, apps) {
                        vm.uso.hoje().asReversed().mapNotNull { porChave[it.chave] }.distinct().take(5)
                    }
                    val contagem = remember(avisos) { avisos.groupingBy { it.pacote }.eachCount() }
                    Gaveta(
                        apps, acoesApp, busca, { busca = it }, recentes, contagem,
                        aoAcao = { a -> a.executar(contexto); vm.recarregarNotas(); vm.timerTalvezMudou(); voltarAoInicio() },
                        aoWeb = { Apps.buscarNaWeb(contexto, it); voltarAoInicio() },
                        contatos = contatos,
                        aoLigar = { if (Contatos.ligar(contexto, it)) voltarAoInicio() },
                        aoMensagem = { if (Contatos.mensagem(contexto, it)) voltarAoInicio() },
                        // vindo do buscador ("Todos os apps"), abre para olhar, sem teclado
                        autoFoco = false,
                    )
                }
                atual == Telas.COMANDO -> {
                    val porChave = remember(apps) { apps.associateBy { it.chave } }
                    val recentes = remember(usoMudou, apps) {
                        vm.uso.hoje().asReversed().mapNotNull { porChave[it.chave] }.distinct().take(5)
                    }
                    BuscaTela(
                        texto = comando,
                        aoMudar = { comando = it },
                        apps = apps,
                        recentes = recentes.ifEmpty { favoritosApps.take(5) },
                        conversas = daMatilha.ifEmpty { conversasComNome },
                        contatos = contatos,
                        acoesApp = acoesApp,
                        acoes = AcoesBusca(
                            abrirConversa = acoesConversa.abrir,
                            ligar = { if (Contatos.ligar(contexto, it)) voltarAoInicio() },
                            mensagem = { if (Contatos.mensagem(contexto, it)) voltarAoInicio() },
                            executar = { a -> a.executar(contexto); vm.recarregarNotas(); vm.timerTalvezMudou(); voltarAoInicio() },
                            web = { Apps.buscarNaWeb(contexto, it); voltarAoInicio() },
                            ajustes = { comando = ""; paginaAjustes = PaginasAjustes.PRINCIPAL; tela = Telas.AJUSTES },
                            todosApps = { busca = ""; tela = Telas.GAVETA },
                        ),
                    )
                }
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
                        matilhaPeloWhatsapp = fonteMatilha != FonteMatilha.AGENDA && conversas.isNotEmpty(),
                        fonteMatilha = fonteMatilha,
                        fontesMatilha = vm.fontesMatilha,
                        mostrarPlayer = mostrarPlayer,
                        detalhePlayer = detalheMusica,
                        modoLista = modoLista,
                        tempoLigado = tempoLigado,
                        cidade = cidade?.descricao,
                        fahrenheit = fahrenheit,
                        compromissos = mostrarCompromissos,
                        fundoAnimado = fundoAnimado,
                        estiloRelogio = estiloRelogio,
                        estiloIcones = vm.estiloIcones.collectAsStateWithLifecycle().value,
                        pacotesIcones = vm.pacotesIcones.collectAsStateWithLifecycle().value,
                        paleta = vm.paleta.collectAsStateWithLifecycle().value,
                        coresWallpaper = vm.coresWallpaper.collectAsStateWithLifecycle().value,
                        ritmo = ritmo,
                        modoNoite = modoNoite,
                        janelaNoite = janelaNoite,
                        matilhaSoFixadas = matilhaSoFixadas,
                        matilhaOrganizar = matilhaOrganizar,
                        mostrarMarca = mostrarMarca,
                        escuroInicio = escuroInicio,
                        gestos = gestos,
                        bloqueioLigado = bloqueioLigado,
                        favoritos = favoritosChaves.mapNotNull { porChaveApp[it] },
                        todosApps = apps,
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
                        fonteMatilha = vm::definirFonteMatilha,
                        mostrarPlayer = vm::definirMostrarPlayer,
                        modoLista = vm::definirModoLista,
                        tempoLigado = vm::definirTempoLigado,
                        escolherCidade = vm::definirCidade,
                        fahrenheit = vm::definirFahrenheit,
                        compromissos = vm::definirCompromissos,
                        fundoAnimado = vm::definirFundoAnimado,
                        estiloRelogio = vm::definirEstiloRelogio,
                        estiloIcones = vm::definirEstiloIcones,
                        baixarPacotes = {
                            Apps.abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack")))
                                || Apps.abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")))
                        },
                        paleta = vm::definirPaleta,
                        ritmo = vm::definirRitmo,
                        modoNoite = vm::definirModoNoite,
                        janelaNoite = vm::definirJanelaNoite,
                        adicionarFavorito = vm::adicionarFavorito,
                        tirarFavorito = vm::alternarFavorito,
                        matilhaSoFixadas = vm::definirMatilhaSoFixadas,
                        matilhaOrganizar = vm::definirMatilhaOrganizar,
                        mostrarMarca = vm::definirMostrarMarca,
                        escuroInicio = vm::definirEscuroInicio,
                        gesto = vm::definirGesto,
                        ligarBloqueio = { Bloqueio.pedirPermissao(contexto) },
                        mostrarApp = { vm.esconder(it, false) },
                        abrirCodigo = { Apps.abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TioDanoni/wolf-launcher"))) },
                    ),
                    paginaInicial = paginaAjustes,
                )
                else -> Unit
            }
        }
        // a previsao da semana, por cima de tudo
        if (mostrandoSemana) {
            val p = previsao
            if (p != null) PrevisaoSemana(p, if (fahrenheit) "°F" else "°") { mostrandoSemana = false } else mostrandoSemana = false
        }
    }
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
    val e = eventos.firstOrNull { !it.diaInteiro && it.inicio - agora < 3 * 3_600_000L } ?: return null
    val quando = if (e.inicio <= agora) "agora" else quandoFalta(e.inicio, agora)
    return if (e.link != null) FaixaInfo("◷", "${e.titulo} · $quando", "Entrar") { entrar(e) }
    else FaixaInfo("◷", "${e.titulo} · $quando", "Ver") { abrir(e) }
}
