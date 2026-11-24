package org.wolfos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.atualizacao.Atualizador
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Cidade
import org.wolfos.launcher.dados.FonteMatilha
import org.wolfos.launcher.dados.TipoMatilha

/** Tudo o que os ajustes mostram. */
class EstadoAjustes(
    val atualizacao: EstadoAtualizacao,
    val instalarSozinho: Boolean,
    val ehPadrao: Boolean,
    val avisosPermitidos: Boolean,
    val temAgenda: Boolean,
    val temContatos: Boolean,
    val mostrarMatilha: Boolean,
    val tipoMatilha: TipoMatilha,
    val matilhaPeloWhatsapp: Boolean,
    val fonteMatilha: FonteMatilha,
    val fontesMatilha: List<FonteMatilha>,
    val mostrarPlayer: Boolean,
    /** O que o player esta vendo agora ("tocando: X (app)", "nenhum app de musica aberto"...). */
    val detalhePlayer: String,
    val tempoLigado: Boolean,
    /** "Piracicaba, Sao Paulo, Brasil" (nulo: ainda sem cidade). */
    val cidade: String?,
    val fahrenheit: Boolean,
    val compromissos: Boolean,
    val fundoAnimado: Boolean,
    val paleta: Paleta,
    val estiloRelogio: EstiloRelogio,
    /** ORIGINAL, LINHA, PALETA, CINZA ou "pack:<pacote>". */
    val estiloIcones: String,
    val pacotesIcones: List<org.wolfos.launcher.dados.PacoteIcones>,
    val ritmo: Boolean,
    val modoNoite: Boolean,
    val janelaNoite: Pair<Int, Int>,
    val doisToques: Boolean,
    val bloqueioLigado: Boolean,
    /** Os favoritos do painel (os escolhidos; vazio = o launcher escolhe pelos mais usados). */
    val favoritos: List<AppInfo>,
    /** Todos os apps (para adicionar favoritos). */
    val todosApps: List<AppInfo>,
    val appsEscondidos: List<AppInfo>,
)

/** O que cada ajuste faz. */
class AcoesAjustes(
    val fechar: () -> Unit,
    val atualizar: () -> Unit,
    val procurar: ((Boolean) -> Unit) -> Unit,
    val instalarSozinho: (Boolean) -> Unit,
    val definirPadrao: () -> Unit,
    val permitirAvisos: () -> Unit,
    val permitirAgenda: () -> Unit,
    val permitirContatos: () -> Unit,
    val informacoesDoLauncher: () -> Unit,
    val papelDeParede: () -> Unit,
    val mostrarMatilha: (Boolean) -> Unit,
    val tipoMatilha: (TipoMatilha) -> Unit,
    val fonteMatilha: (FonteMatilha) -> Unit,
    val mostrarPlayer: (Boolean) -> Unit,
    val tempoLigado: (Boolean) -> Unit,
    val escolherCidade: (Cidade) -> Unit,
    val fahrenheit: (Boolean) -> Unit,
    val compromissos: (Boolean) -> Unit,
    val fundoAnimado: (Boolean) -> Unit,
    val paleta: (Paleta) -> Unit,
    val estiloRelogio: (EstiloRelogio) -> Unit,
    val estiloIcones: (String) -> Unit,
    val baixarPacotes: () -> Unit,
    val ritmo: (Boolean) -> Unit,
    val modoNoite: (Boolean) -> Unit,
    val janelaNoite: (Int, Int) -> Unit,
    val adicionarFavorito: (AppInfo) -> Unit,
    val tirarFavorito: (AppInfo) -> Unit,
    val doisToques: (Boolean) -> Unit,
    val ligarBloqueio: () -> Unit,
    val mostrarApp: (AppInfo) -> Unit,
    val abrirCodigo: () -> Unit,
)

/** As paginas dos ajustes (a tela principal pode abrir direto numa delas). */
object PaginasAjustes {
    const val PRINCIPAL = "principal"
    const val FAVORITOS = "favoritos"
    const val ESCONDIDOS = "escondidos"
    const val GESTOS = "gestos"
}

/** Os ajustes do launcher: uma pagina principal e paginas para favoritos, apps escondidos e gestos. */
@Composable
fun AjustesTela(estado: EstadoAjustes, acoes: AcoesAjustes, paginaInicial: String = PaginasAjustes.PRINCIPAL) {
    var pagina by rememberSaveable { mutableStateOf(paginaInicial) }
    // voltar numa pagina de dentro volta para a principal
    BackHandler(enabled = pagina != PaginasAjustes.PRINCIPAL) { pagina = PaginasAjustes.PRINCIPAL }
    val voltar = { pagina = PaginasAjustes.PRINCIPAL }

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        when (pagina) {
            PaginasAjustes.FAVORITOS -> PaginaFavoritos(estado, acoes, voltar)
            PaginasAjustes.ESCONDIDOS -> PaginaEscondidos(estado, acoes, voltar)
            PaginasAjustes.GESTOS -> PaginaGestos(voltar)
            else -> PaginaPrincipal(estado, acoes) { pagina = it }
        }
    }
}

@Composable
private fun PaginaPrincipal(estado: EstadoAjustes, acoes: AcoesAjustes, abrir: (String) -> Unit) {
    var procurando by remember { mutableStateOf<String?>(null) }
    Cabecalho("Ajustes", acoes.fechar)

    textoAtualizacao(estado.atualizacao)?.let { (simbolo, texto, botao) ->
        Faixa(simbolo, texto, botao, acoes.atualizar, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
    }
    if (!estado.ehPadrao) {
        LinhaMenu(Icons.Outlined.Home, "Usar como tela inicial", "o WolfLauncher ainda não é a tela inicial", fim = { Seta() }, aoTocar = acoes.definirPadrao)
    }

    SecaoMenu("Relógio")
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        for (e in EstiloRelogio.entries) {
            val marcado = e == estado.estiloRelogio
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (marcado) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .border(1.dp, if (marcado) MaterialTheme.colorScheme.primary else Texto.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { acoes.estiloRelogio(e) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    if (e == EstiloRelogio.MANUSCRITO) "Ter." else "12",
                    fontFamily = fonteDoEstilo(e),
                    fontSize = 30.sp,
                    fontWeight = if (e == EstiloRelogio.CLASSICO) FontWeight.Light else FontWeight.Normal,
                    style = androidx.compose.ui.text.TextStyle(brush = Brush.verticalGradient(listOf(Texto, MaterialTheme.colorScheme.primary))),
                )
                Text(e.titulo, color = if (marcado) Texto else TextoFraco, fontSize = 11.sp, maxLines = 1)
            }
        }
    }

    SecaoMenu("Ícones")
    // a previa: alguns apps de verdade, no estilo escolhido
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        for (app in (estado.favoritos + estado.todosApps).distinctBy { it.chave }.take(6)) IconeApp(app, 40.dp)
    }
    val estilos = listOf("ORIGINAL" to "Coloridos", "LINHA" to "Em linha", "PALETA" to "Na cor da paleta", "CINZA" to "Cinza suave")
    Pilulas(estilos.map { it.second }, estilos.indexOfFirst { it.first == estado.estiloIcones }) { acoes.estiloIcones(estilos[it].first) }
    Text(
        "“Em linha” e “Na cor da paleta” usam o ícone temático do Android 13+; app sem ele aparece em cinza.",
        color = TextoFraco, fontSize = 12.sp, modifier = Modifier.padding(start = 78.dp, end = 24.dp, bottom = 4.dp),
    )
    for (p in estado.pacotesIcones) {
        val marcado = estado.estiloIcones == "pack:" + p.pacote
        LinhaMenu(
            Icons.Outlined.Palette, p.nome, if (marcado) "em uso" else "pacote de ícones",
            fim = { if (marcado) Valor("✓") },
        ) { acoes.estiloIcones("pack:" + p.pacote) }
    }
    LinhaMenu(
        Icons.Outlined.Download, "Baixar pacotes de ícones",
        if (estado.pacotesIcones.isEmpty()) "nenhum instalado; a Play Store tem muitos (padrão Nova/ADW)" else "mais na Play Store",
        fim = { Seta() }, aoTocar = acoes.baixarPacotes,
    )

    SecaoMenu("Cores")
    Text(
        "As cores do WolfLauncher (relógio, traços, busca, player, aurora). À noite fica lilás.",
        color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp),
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        for (p in Paleta.entries) {
            val marcada = p == estado.paleta
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { acoes.paleta(p) }.padding(6.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(p.primaria, p.secundaria)))
                        .border(if (marcada) 3.dp else 0.dp, if (marcada) Texto else Color.Transparent, CircleShape),
                )
                Text(
                    p.titulo, color = if (marcada) Texto else TextoFraco, fontSize = 11.5.sp,
                    fontWeight = if (marcada) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    SecaoMenu("Topo")
    var escolhendoCidade by remember { mutableStateOf(false) }
    LinhaMenu(
        Icons.Outlined.WbSunny, "Clima", "temperatura, máxima e mínima embaixo do relógio (Open-Meteo)",
        fim = { ChaveWolf(estado.tempoLigado, acoes.tempoLigado) },
    ) { acoes.tempoLigado(!estado.tempoLigado) }
    if (estado.tempoLigado) {
        LinhaMenu(
            Icons.Outlined.LocationCity, "Cidade",
            estado.cidade ?: "escolha uma cidade",
            fim = { Seta() },
        ) { escolhendoCidade = true }
        Pilulas(listOf("Celsius (°C)", "Fahrenheit (°F)"), if (estado.fahrenheit) 1 else 0) { acoes.fahrenheit(it == 1) }
    }
    if (escolhendoCidade) {
        EscolherCidade(aoEscolher = { acoes.escolherCidade(it); escolhendoCidade = false }, aoFechar = { escolhendoCidade = false })
    }
    LinhaMenu(
        Icons.Outlined.EventNote, "Compromissos de hoje", "da Google Agenda, embaixo do clima",
        fim = { ChaveWolf(estado.compromissos, acoes.compromissos) },
    ) { acoes.compromissos(!estado.compromissos) }
    LinhaMenu(
        Icons.Outlined.AutoAwesome, "Fundo animado", "a aurora se mexe devagar (gasta um pouquinho de bateria)",
        fim = { ChaveWolf(estado.fundoAnimado, acoes.fundoAnimado) },
    ) { acoes.fundoAnimado(!estado.fundoAnimado) }

    SecaoMenu("Tela inicial")
    LinhaMenu(
        Icons.Outlined.PushPin, "Favoritos",
        if (estado.favoritos.isEmpty()) "nenhum fixado: o painel mostra os mais usados" else "${estado.favoritos.size} fixado(s) no painel",
        fim = { Seta() },
    ) { abrir(PaginasAjustes.FAVORITOS) }
    LinhaMenu(
        Icons.Outlined.MusicNote, "Player de música",
        if (estado.avisosPermitidos) "aparece quando algo toca · agora: ${estado.detalhePlayer}"
        else "precisa do acesso às notificações (em Permissões)",
        fim = { ChaveWolf(estado.mostrarPlayer, acoes.mostrarPlayer) },
    ) { if (estado.avisosPermitidos) acoes.mostrarPlayer(!estado.mostrarPlayer) else acoes.permitirAvisos() }
    LinhaMenu(
        Icons.Outlined.Groups, "Matilha",
        if (estado.matilhaPeloWhatsapp) "as conversas recentes do WhatsApp; segure uma para fixar"
        else "as pessoas da agenda (com o WhatsApp, viram as conversas)",
        fim = { ChaveWolf(estado.mostrarMatilha, acoes.mostrarMatilha) },
    ) { acoes.mostrarMatilha(!estado.mostrarMatilha) }
    if (estado.mostrarMatilha) {
        if (estado.fontesMatilha.size > 1) {
            Text("De onde vêm", color = TextoFraco, fontSize = 12.5.sp, modifier = Modifier.padding(start = 78.dp, bottom = 6.dp))
            Pilulas(estado.fontesMatilha.map { it.titulo }, estado.fontesMatilha.indexOf(estado.fonteMatilha)) {
                acoes.fonteMatilha(estado.fontesMatilha[it])
            }
        }
        if (estado.matilhaPeloWhatsapp) {
            Text("Mostrar", color = TextoFraco, fontSize = 12.5.sp, modifier = Modifier.padding(start = 78.dp, top = 4.dp, bottom = 6.dp))
            Pilulas(TipoMatilha.entries.map { it.titulo }, TipoMatilha.entries.indexOf(estado.tipoMatilha)) {
                acoes.tipoMatilha(TipoMatilha.entries[it])
            }
        }
    }
    LinhaMenu(Icons.Outlined.Wallpaper, "Papel de parede", "a cor de destaque vem dele (Android 12+)", fim = { Seta() }, aoTocar = acoes.papelDeParede)

    SecaoMenu("Ritmo do dia")
    LinhaMenu(
        Icons.Outlined.Schedule, "Sugestões de agora", "a fileira “Agora”: o que você costuma abrir nesta hora, mensagens e música",
        fim = { ChaveWolf(estado.ritmo, acoes.ritmo) },
    ) { acoes.ritmo(!estado.ritmo) }
    LinhaMenu(
        Icons.Outlined.Bedtime, "Modo noite", "cor calma e o despertador no topo",
        fim = { ChaveWolf(estado.modoNoite, acoes.modoNoite) },
    ) { acoes.modoNoite(!estado.modoNoite) }
    if (estado.modoNoite) {
        val janelas = listOf(22 * 60 to 6 * 60, 22 * 60 + 30 to 7 * 60, 23 * 60 to 7 * 60 + 30, 0 to 8 * 60)
        fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
        Pilulas(janelas.map { (a, b) -> "${hhmm(a)} – ${hhmm(b)}" }, janelas.indexOf(estado.janelaNoite)) { i ->
            acoes.janelaNoite(janelas[i].first, janelas[i].second)
        }
    }

    SecaoMenu("Apps")
    LinhaMenu(
        Icons.Outlined.VisibilityOff, "Apps escondidos",
        if (estado.appsEscondidos.isEmpty()) "nenhum; segure um app para esconder" else "${estado.appsEscondidos.size} escondido(s)",
        fim = { Seta() },
    ) { abrir(PaginasAjustes.ESCONDIDOS) }
    LinhaMenu(Icons.Outlined.Gesture, "Gestos", "o que cada movimento faz", fim = { Seta() }) { abrir(PaginasAjustes.GESTOS) }
    LinhaMenu(
        Icons.Outlined.Lock, "Tocar duas vezes para bloquear",
        when {
            !estado.doisToques -> "desligado"
            estado.bloqueioLigado -> "num espaço vazio da tela inicial"
            else -> "falta ligar na acessibilidade: toque aqui"
        },
        fim = { ChaveWolf(estado.doisToques, acoes.doisToques) },
    ) { if (estado.doisToques && !estado.bloqueioLigado) acoes.ligarBloqueio() else acoes.doisToques(!estado.doisToques) }

    SecaoMenu("Permissões")
    LinhaMenu(
        Icons.Outlined.NotificationsNone, "Notificações",
        "números nos apps, mensagens, Matilha e o player",
        fim = { Valor(if (estado.avisosPermitidos) "ligado" else "permitir") },
        aoTocar = acoes.permitirAvisos,
    )
    if (!estado.avisosPermitidos) {
        LinhaMenu(
            Icons.Outlined.WarningAmber, "O Android disse “configuração restrita”?",
            "toque aqui, depois em ⋮ › Permitir configurações restritas, e volte",
            aoTocar = acoes.informacoesDoLauncher,
        )
    }
    LinhaMenu(
        Icons.Outlined.CalendarMonth, "Agenda", "o próximo compromisso e o seu dia",
        fim = { Valor(if (estado.temAgenda) "ligado" else "permitir") },
        aoTocar = if (estado.temAgenda) null else acoes.permitirAgenda,
    )
    LinhaMenu(
        Icons.Outlined.Contacts, "Contatos", "“ligar mãe” na busca e a Matilha sem WhatsApp",
        fim = { Valor(if (estado.temContatos) "ligado" else "permitir") },
        aoTocar = if (estado.temContatos) null else acoes.permitirContatos,
    )

    SecaoMenu("Atualizações")
    LinhaMenu(Icons.Outlined.Update, "Procurar atualização agora", procurando ?: "versão instalada: ${Atualizador.versaoAtual}") {
        procurando = "procurando…"
        acoes.procurar { achou ->
            procurando = if (achou) "tem versão nova: veja o aviso acima" else "você já está na última (${Atualizador.versaoAtual})"
        }
    }
    LinhaMenu(
        Icons.Outlined.SystemUpdate, "Instalar sozinho", "baixa e instala a versão nova sem precisar de você",
        fim = { ChaveWolf(estado.instalarSozinho, acoes.instalarSozinho) },
    ) { acoes.instalarSozinho(!estado.instalarSozinho) }

    SecaoMenu("Sobre")
    LinhaMenu(Icons.Outlined.Info, "WolfLauncher ${Atualizador.versaoAtual}", "a tela inicial do WolfOS")
    LinhaMenu(Icons.Outlined.Code, "Código no GitHub", "github.com/TioDanoni/wolf-launcher", fim = { Seta() }, aoTocar = acoes.abrirCodigo)
}

@Composable
private fun PaginaFavoritos(estado: EstadoAjustes, acoes: AcoesAjustes, voltar: () -> Unit) {
    var adicionando by remember { mutableStateOf(false) }
    Cabecalho("Favoritos", voltar)
    Text(
        "Os apps fixos do painel da tela inicial (até 8). Sem nenhum fixado, o painel mostra os que você mais usa. " +
            "Também dá para fixar segurando um app › Fixar nos favoritos.",
        color = TextoFraco, fontSize = 13.5.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
    for (app in estado.favoritos) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            IconeApp(app, 40.dp)
            Text(app.nome, color = Texto, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
            Pilula("Tirar", destaque = false) { acoes.tirarFavorito(app) }
        }
    }
    if (estado.favoritos.size < 8) {
        LinhaMenu(Icons.Outlined.Add, "Adicionar favorito", fim = { Seta() }) { adicionando = true }
    }
    if (adicionando) {
        EscolherApps(
            titulo = "Adicionar aos favoritos",
            apps = estado.todosApps.filter { a -> estado.favoritos.none { it.chave == a.chave } },
            aoEscolher = { acoes.adicionarFavorito(it) },
            aoFechar = { adicionando = false },
        )
    }
}

@Composable
private fun PaginaEscondidos(estado: EstadoAjustes, acoes: AcoesAjustes, voltar: () -> Unit) {
    Cabecalho("Apps escondidos", voltar)
    if (estado.appsEscondidos.isEmpty()) {
        Text(
            "Nenhum app escondido. Para esconder, segure um app (no painel, na lista ou na busca) › Esconder.",
            color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
    for (app in estado.appsEscondidos) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            IconeApp(app, 40.dp)
            Text(app.nome, color = Texto, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
            Pilula("Mostrar", destaque = true) { acoes.mostrarApp(app) }
        }
    }
}

@Composable
private fun PaginaGestos(voltar: () -> Unit) {
    Cabecalho("Gestos", voltar)
    LinhaMenu(Icons.Outlined.KeyboardArrowUp, "Deslizar para cima", "o buscador: apps, conversas, pessoas, contas, ajustes e a web")
    LinhaMenu(Icons.Outlined.KeyboardArrowDown, "Deslizar para baixo", "a cortina de notificações")
    LinhaMenu(Icons.Outlined.SwipeRight, "Deslizar para a direita", "seu dia: antes, agora e depois")
    LinhaMenu(Icons.Outlined.SwipeLeft, "Deslizar para a esquerda", "o painel de widgets")
    LinhaMenu(Icons.Outlined.Lock, "Tocar duas vezes", "bloqueia a tela (liga em Ajustes)")
    LinhaMenu(Icons.Outlined.Apps, "Segurar um app", "mensagens, atalhos, favoritos, renomear, esconder, desinstalar")
    LinhaMenu(Icons.Outlined.Home, "Segurar um espaço vazio", "o menu do launcher")
    LinhaMenu(Icons.Outlined.Search, "Barra de busca", "apps, pessoas, contas, timer, alarme, lanterna, ajustes do Android")
}

/** Opcoes em pilulas, uma marcada. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Pilulas(opcoes: List<String>, marcada: Int, aoEscolher: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 78.dp, end = 24.dp, bottom = 8.dp),
    ) {
        opcoes.forEachIndexed { i, o -> Pilula(o, destaque = i == marcada) { aoEscolher(i) } }
    }
}
