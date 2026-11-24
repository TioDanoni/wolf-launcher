package org.wolfos.launcher

import android.Manifest
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
import org.wolfos.launcher.dados.Agenda
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Matilha
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.Ritmo
import org.wolfos.launcher.dados.Uso
import org.wolfos.launcher.dados.Verbo
import org.wolfos.launcher.dados.agrupar
import org.wolfos.launcher.dados.verboDe
import org.wolfos.launcher.ui.AcoesApp
import org.wolfos.launcher.ui.AcoesAviso
import org.wolfos.launcher.ui.AcoesDia
import org.wolfos.launcher.ui.DiaTela
import org.wolfos.launcher.ui.FaixaInfo
import org.wolfos.launcher.ui.Passado
import org.wolfos.launcher.ui.agora
import org.wolfos.launcher.ui.quandoFalta
import org.wolfos.launcher.ui.AcoesPessoa
import org.wolfos.launcher.ui.FileiraMatilha
import org.wolfos.launcher.ui.Previa
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
}

/** As telas do launcher. O verbo aberto vai junto no nome ("verbo:OUVIR"). */
private object Telas {
    const val INICIO = "inicio"
    const val GAVETA = "gaveta"
    const val COMANDO = "comando"
    const val AJUSTES = "ajustes"
    const val DIA = "dia"
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
    val ritmo by vm.ritmo.collectAsStateWithLifecycle()
    val modoNoite by vm.modoNoite.collectAsStateWithLifecycle()
    val janelaNoite by vm.janelaNoite.collectAsStateWithLifecycle()
    val fone by vm.fone.collectAsStateWithLifecycle()
    val eventos by vm.eventos.collectAsStateWithLifecycle()
    val temAgenda by vm.temAgenda.collectAsStateWithLifecycle()
    val alarme by vm.alarme.collectAsStateWithLifecycle()
    val usoMudou by vm.usoMudou.collectAsStateWithLifecycle()

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

    // escolher alguem da agenda para a Matilha (o seletor do Android nao precisa de permissao,
    // mas o numero vem da nossa leitura da agenda, que precisa)
    val escolherPessoa = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.lastPathSegment?.toLongOrNull()?.let(vm::adicionarNaMatilha)
    }

    val pedidoAgenda = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.carregarDia()
    }

    val todosOsVerbos = remember(apps, escolhas) { agrupar(apps, escolhas) }
    // a ordem do momento: hora do dia, costume da pessoa, fone, modo noite
    val pontos = remember(hora, usoMudou) { vm.uso.pontosDosVerbos(hora) }
    val verbos = remember(todosOsVerbos, hora, pontos, fone, noite, ritmo) {
        Ritmo.ordenar(todosOsVerbos, hora, pontos, fone, noite, ritmo)
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
        abrirDia = { vm.carregarDia(); tela = Telas.DIA },
    )
    val abrirAlarmes = { Apps.abrirTela(contexto, Intent(AlarmClock.ACTION_SHOW_ALARMS)); Unit }
    val faixaAgora = remember(noite, alarme, eventos, hora) {
        faixaDoAgora(noite, alarme, eventos, abrirAlarmes, { Agenda.entrar(contexto, it) }, { Agenda.abrir(contexto, it) })
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
                when {
                    targetState == Telas.DIA -> slideInHorizontally { -it / 3 } + fadeIn() togetherWith fadeOut()
                    initialState == Telas.DIA -> fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                    targetState == Telas.INICIO -> fadeIn() togetherWith slideOutVertically { it / 4 } + fadeOut()
                    else -> slideInVertically { it / 4 } + fadeIn() togetherWith fadeOut()
                }
            },
            label = "tela",
        ) { atual ->
            when {
                atual == Telas.INICIO -> Inicio(apps, verbos, atualizacao, ehPadrao, previas, faixaAgora, noite, acoesInicio) {
                    if (mostrarMatilha) FileiraMatilha(pessoas, avisos, temContatos, acoesPessoa)
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
                    avisosPermitidos = avisosPermitidos,
                    aoPermitirAvisos = { Avisos.pedirPermissao(contexto) },
                    aoInformacoesDoLauncher = {
                        Apps.abrirTela(
                            contexto,
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + contexto.packageName)),
                        )
                    },
                    mostrarMatilha = mostrarMatilha,
                    aoMostrarMatilha = vm::definirMostrarMatilha,
                    ritmo = ritmo,
                    aoRitmo = vm::definirRitmo,
                    modoNoite = modoNoite,
                    aoModoNoite = vm::definirModoNoite,
                    janelaNoite = janelaNoite,
                    aoJanelaNoite = vm::definirJanelaNoite,
                    aoFechar = voltarAoInicio,
                )
                atual.startsWith(Telas.VERBO) -> {
                    val verbo = Verbo.entries.firstOrNull { it.name == atual.removePrefix(Telas.VERBO) }
                    val lista = verbo?.let { verbos[it] }.orEmpty()
                    if (verbo != null) VerboTela(verbo, lista, avisos, avisosPermitidos, acoesApp, acoesAviso, voltarAoInicio)
                }
                else -> Unit
            }
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
