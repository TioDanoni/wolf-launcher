package org.wolfos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wolfos.launcher.atualizacao.Atualizador
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.dados.AcaoGesto
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Cidade
import org.wolfos.launcher.dados.FonteMatilha
import org.wolfos.launcher.dados.Gesto
import org.wolfos.launcher.dados.PacoteIcones
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
    val matilhaSoFixadas: Boolean,
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
    /** As cores que o launcher tirou do wallpaper (para a amostra do "Do wallpaper"). */
    val coresWallpaper: Pair<Color, Color>?,
    val estiloRelogio: EstiloRelogio,
    /** ORIGINAL, LINHA, PALETA, CINZA ou "pack:<pacote>". */
    val estiloIcones: String,
    val pacotesIcones: List<PacoteIcones>,
    /** As sugestoes de agora na lista da tela inicial. */
    val ritmo: Boolean,
    val modoNoite: Boolean,
    val janelaNoite: Pair<Int, Int>,
    val mostrarMarca: Boolean,
    /** LEVE, MEDIO ou FORTE. */
    val escuroInicio: String,
    val gestos: Map<Gesto, AcaoGesto>,
    val bloqueioLigado: Boolean,
    /** Os favoritos escolhidos (vazio = o launcher usa os mais usados). */
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
    val matilhaSoFixadas: (Boolean) -> Unit,
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
    val mostrarMarca: (Boolean) -> Unit,
    val escuroInicio: (String) -> Unit,
    val gesto: (Gesto, AcaoGesto) -> Unit,
    val adicionarFavorito: (AppInfo) -> Unit,
    val tirarFavorito: (AppInfo) -> Unit,
    val ligarBloqueio: () -> Unit,
    val mostrarApp: (AppInfo) -> Unit,
    val abrirCodigo: () -> Unit,
)

/** As paginas de dentro dos ajustes (a tela principal pode abrir direto numa delas). */
object PaginasAjustes {
    const val PRINCIPAL = "principal"
    const val FAVORITOS = "favoritos"
    const val ESCONDIDOS = "escondidos"
}

/** As abas dos ajustes. */
private enum class Aba(val titulo: String, val icone: ImageVector) {
    INICIO("Início", Icons.Outlined.Home),
    VISUAL("Visual", Icons.Outlined.Palette),
    MATILHA("Matilha", Icons.Outlined.Pets),
    GESTOS("Gestos", Icons.Outlined.Gesture),
    APPS("Apps", Icons.Outlined.Apps),
    SISTEMA("Sistema", Icons.Outlined.Settings),
}

/**
 * Os ajustes em abas (Inicio, Visual, Matilha, Gestos, Apps, Sistema): tocar na aba ou
 * deslizar para o lado troca. Favoritos e apps escondidos abrem numa pagina propria.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AjustesTela(estado: EstadoAjustes, acoes: AcoesAjustes, paginaInicial: String = PaginasAjustes.PRINCIPAL) {
    var pagina by rememberSaveable { mutableStateOf(paginaInicial) }
    BackHandler(enabled = pagina != PaginasAjustes.PRINCIPAL) { pagina = PaginasAjustes.PRINCIPAL }
    val voltar = { pagina = PaginasAjustes.PRINCIPAL }

    when (pagina) {
        PaginasAjustes.FAVORITOS -> Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            PaginaFavoritos(estado, acoes, voltar)
        }
        PaginasAjustes.ESCONDIDOS -> Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            PaginaEscondidos(estado, acoes, voltar)
        }
        else -> {
            val abas = Aba.entries
            val paginas = rememberPagerState { abas.size }
            val escopo = rememberCoroutineScope()
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Cabecalho("Ajustes", acoes.fechar)
                textoAtualizacao(estado.atualizacao)?.let { (simbolo, texto, botao) ->
                    Faixa(simbolo, texto, botao, acoes.atualizar, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                BarraDeAbas(abas, paginas.currentPage) { i -> escopo.launch { paginas.animateScrollToPage(i) } }
                HorizontalPager(paginas, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { i ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        when (abas[i]) {
                            Aba.INICIO -> AbaInicio(estado, acoes) { pagina = it }
                            Aba.VISUAL -> AbaVisual(estado, acoes)
                            Aba.MATILHA -> AbaMatilha(estado, acoes)
                            Aba.GESTOS -> AbaGestos(estado, acoes)
                            Aba.APPS -> AbaApps(estado) { pagina = it }
                            Aba.SISTEMA -> AbaSistema(estado, acoes)
                        }
                    }
                }
            }
        }
    }
}

/** As abas em pilulas que rolam para o lado; a marcada fica com o degrade da paleta. */
@Composable
private fun BarraDeAbas(abas: List<Aba>, atual: Int, aoEscolher: (Int) -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        abas.forEachIndexed { i, a ->
            val marcada = i == atual
            val largura by animateDpAsState(if (marcada) 1.5.dp else 1.dp, label = "borda")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (marcada) Brush.linearGradient(listOf(cor.copy(alpha = 0.28f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(largura, if (marcada) cor else Texto.copy(alpha = 0.14f), CircleShape)
                    .clickable { aoEscolher(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(a.icone, contentDescription = null, tint = if (marcada) cor else TextoFraco, modifier = Modifier.size(18.dp))
                Text(
                    a.titulo, color = if (marcada) Texto else TextoFraco, fontSize = 14.sp,
                    fontWeight = if (marcada) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Inicio

@Composable
private fun AbaInicio(estado: EstadoAjustes, acoes: AcoesAjustes, abrir: (String) -> Unit) {
    SecaoMenu("Relógio")
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        for (e in EstiloRelogio.entries) {
            val marcado = e == estado.estiloRelogio
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (marcado) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .border(1.dp, if (marcado) MaterialTheme.colorScheme.primary else Texto.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { acoes.estiloRelogio(e) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    when (e) {
                        EstiloRelogio.MANUSCRITO -> "Ter."
                        else -> "12"
                    },
                    fontFamily = fonteDoEstilo(e),
                    fontSize = 30.sp,
                    fontWeight = if (e == EstiloRelogio.CLASSICO || e == EstiloRelogio.VERTICAL) FontWeight.Light else FontWeight.Normal,
                    style = androidx.compose.ui.text.TextStyle(brush = Brush.verticalGradient(listOf(Texto, MaterialTheme.colorScheme.primary))),
                )
                Text(e.titulo, color = if (marcado) Texto else TextoFraco, fontSize = 11.sp, maxLines = 1)
            }
        }
    }

    SecaoMenu("Clima")
    var escolhendoCidade by remember { mutableStateOf(false) }
    LinhaMenu(
        Icons.Outlined.WbSunny, "Mostrar o clima", "embaixo do relógio (Open-Meteo, sem cadastro)",
        fim = { ChaveWolf(estado.tempoLigado, acoes.tempoLigado) },
    ) { acoes.tempoLigado(!estado.tempoLigado) }
    if (estado.tempoLigado) {
        LinhaMenu(Icons.Outlined.LocationCity, "Cidade", estado.cidade ?: "escolha uma cidade", fim = { Seta() }) { escolhendoCidade = true }
        Pilulas(listOf("Celsius (°C)", "Fahrenheit (°F)"), if (estado.fahrenheit) 1 else 0) { acoes.fahrenheit(it == 1) }
    }
    if (escolhendoCidade) {
        EscolherCidade(aoEscolher = { acoes.escolherCidade(it); escolhendoCidade = false }, aoFechar = { escolhendoCidade = false })
    }

    SecaoMenu("Hoje")
    LinhaMenu(
        Icons.Outlined.EventNote, "Compromissos de hoje", "da Google Agenda; só aparecem quando há algo no dia",
        fim = { ChaveWolf(estado.compromissos, acoes.compromissos) },
    ) { acoes.compromissos(!estado.compromissos) }
    LinhaMenu(
        Icons.Outlined.MusicNote, "Player de música",
        if (estado.avisosPermitidos) "quando algo toca · agora: ${estado.detalhePlayer}" else "precisa do acesso às notificações (aba Sistema)",
        fim = { ChaveWolf(estado.mostrarPlayer, acoes.mostrarPlayer) },
    ) { if (estado.avisosPermitidos) acoes.mostrarPlayer(!estado.mostrarPlayer) else acoes.permitirAvisos() }

    SecaoMenu("Lista de apps")
    LinhaMenu(
        Icons.Outlined.PushPin, "Favoritos",
        if (estado.favoritos.isEmpty()) "nenhum fixado: a lista mostra os mais usados" else "${estado.favoritos.size} fixado(s)",
        fim = { Seta() },
    ) { abrir(PaginasAjustes.FAVORITOS) }
    LinhaMenu(
        Icons.Outlined.Schedule, "Sugestões de agora",
        "completa a lista com o que você costuma abrir nesta hora, mensagens e música",
        fim = { ChaveWolf(estado.ritmo, acoes.ritmo) },
    ) { acoes.ritmo(!estado.ritmo) }
}

// ---------------------------------------------------------------- Visual

@Composable
private fun AbaVisual(estado: EstadoAjustes, acoes: AcoesAjustes) {
    SecaoMenu("Cores")
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
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
                        .background(
                            Brush.linearGradient(
                                if (p == Paleta.WALLPAPER && estado.coresWallpaper != null) estado.coresWallpaper.toList()
                                else listOf(p.primaria, p.secundaria),
                            ),
                        )
                        .border(if (marcada) 3.dp else 0.dp, if (marcada) Texto else Color.Transparent, CircleShape),
                )
                Text(
                    p.titulo, color = if (marcada) Texto else TextoFraco, fontSize = 11.5.sp,
                    fontWeight = if (marcada) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    SecaoMenu("Ícones")
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
        color = TextoFraco, fontSize = 12.sp, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
    )
    for (p in estado.pacotesIcones) {
        val marcado = estado.estiloIcones == "pack:" + p.pacote
        LinhaMenu(Icons.Outlined.Palette, p.nome, if (marcado) "em uso" else "pacote de ícones", fim = { if (marcado) Valor("✓") }) {
            acoes.estiloIcones("pack:" + p.pacote)
        }
    }
    LinhaMenu(
        Icons.Outlined.Download, "Baixar pacotes de ícones",
        if (estado.pacotesIcones.isEmpty()) "nenhum instalado; a Play Store tem muitos (padrão Nova/ADW)" else "mais na Play Store",
        fim = { Seta() }, aoTocar = acoes.baixarPacotes,
    )

    SecaoMenu("Fundo")
    LinhaMenu(Icons.Outlined.Brightness6, "Escurecer o wallpaper", "o quanto a tela inicial escurece para o texto ler bem")
    val niveis = listOf("LEVE" to "Leve", "MEDIO" to "Médio", "FORTE" to "Forte")
    Pilulas(niveis.map { it.second }, niveis.indexOfFirst { it.first == estado.escuroInicio }) { acoes.escuroInicio(niveis[it].first) }
    LinhaMenu(
        Icons.Outlined.AutoAwesome, "Fundo animado", "a aurora se mexe devagar (gasta um pouquinho de bateria)",
        fim = { ChaveWolf(estado.fundoAnimado, acoes.fundoAnimado) },
    ) { acoes.fundoAnimado(!estado.fundoAnimado) }
    LinhaMenu(Icons.Outlined.Wallpaper, "Papel de parede", "a cor da paleta “Do wallpaper” vem dele", fim = { Seta() }, aoTocar = acoes.papelDeParede)
    LinhaMenu(
        Icons.Outlined.Tune, "Marca no topo", "“WolfLauncher” com o lobo, em cima do relógio",
        fim = { ChaveWolf(estado.mostrarMarca, acoes.mostrarMarca) },
    ) { acoes.mostrarMarca(!estado.mostrarMarca) }

    SecaoMenu("Modo noite")
    LinhaMenu(
        Icons.Outlined.Bedtime, "Modo noite", "cor lilás e calma, e o despertador no topo",
        fim = { ChaveWolf(estado.modoNoite, acoes.modoNoite) },
    ) { acoes.modoNoite(!estado.modoNoite) }
    if (estado.modoNoite) {
        val janelas = listOf(22 * 60 to 6 * 60, 22 * 60 + 30 to 7 * 60, 23 * 60 to 7 * 60 + 30, 0 to 8 * 60)
        fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
        Pilulas(janelas.map { (a, b) -> "${hhmm(a)} – ${hhmm(b)}" }, janelas.indexOf(estado.janelaNoite)) { i ->
            acoes.janelaNoite(janelas[i].first, janelas[i].second)
        }
    }
}

// ---------------------------------------------------------------- Matilha

@Composable
private fun AbaMatilha(estado: EstadoAjustes, acoes: AcoesAjustes) {
    SecaoMenu("Matilha")
    LinhaMenu(
        Icons.Outlined.Groups, "Mostrar a Matilha", "as pessoas e conversas no polegar, embaixo da lista",
        fim = { ChaveWolf(estado.mostrarMatilha, acoes.mostrarMatilha) },
    ) { acoes.mostrarMatilha(!estado.mostrarMatilha) }
    if (!estado.mostrarMatilha) return

    if (estado.fontesMatilha.size > 1) {
        SecaoMenu("De onde vêm")
        Pilulas(estado.fontesMatilha.map { it.titulo }, estado.fontesMatilha.indexOf(estado.fonteMatilha)) {
            acoes.fonteMatilha(estado.fontesMatilha[it])
        }
    }
    SecaoMenu("Quais aparecem")
    Pilulas(listOf("Recentes e fixadas", "Só as fixadas"), if (estado.matilhaSoFixadas) 1 else 0) { acoes.matilhaSoFixadas(it == 1) }
    Text(
        if (estado.matilhaSoFixadas) "Só as conversas que você fixou (segure uma › Fixar, ou toque no + da Matilha)."
        else "As fixadas primeiro; depois quem está esperando resposta e as que você mais usa.",
        color = TextoFraco, fontSize = 12.5.sp, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
    )
    if (estado.matilhaPeloWhatsapp) {
        SecaoMenu("Pessoas ou grupos")
        Pilulas(TipoMatilha.entries.map { it.titulo }, TipoMatilha.entries.indexOf(estado.tipoMatilha)) {
            acoes.tipoMatilha(TipoMatilha.entries[it])
        }
    }
}

// ---------------------------------------------------------------- Gestos

@Composable
private fun AbaGestos(estado: EstadoAjustes, acoes: AcoesAjustes) {
    var aberto by remember { mutableStateOf<Gesto?>(null) }
    SecaoMenu("O que cada gesto faz")
    for (g in Gesto.entries) {
        val atual = estado.gestos[g] ?: AcaoGesto.NADA
        val icone = when (g) {
            Gesto.CIMA -> Icons.Outlined.KeyboardArrowUp
            Gesto.BAIXO -> Icons.Outlined.KeyboardArrowDown
            Gesto.DIREITA -> Icons.Outlined.SwipeRight
            Gesto.ESQUERDA -> Icons.Outlined.SwipeLeft
            Gesto.DOIS_TOQUES -> Icons.Outlined.TouchApp
        }
        LinhaMenu(icone, g.titulo, atual.titulo, fim = { Seta() }) { aberto = if (aberto == g) null else g }
        if (aberto == g) {
            // tocar duas vezes nao abre telas de deslizar; o resto pode tudo menos bloquear de lado
            val opcoes = AcaoGesto.entries
            Pilulas(opcoes.map { it.titulo }, opcoes.indexOf(atual)) { i -> acoes.gesto(g, opcoes[i]); aberto = null }
        }
    }
    if (estado.gestos.values.any { it == AcaoGesto.BLOQUEAR } && !estado.bloqueioLigado) {
        LinhaMenu(
            Icons.Outlined.Lock, "Ligar o bloqueio", "para “Bloquear a tela” funcionar, ligue o WolfLauncher na acessibilidade",
            fim = { Seta() }, aoTocar = acoes.ligarBloqueio,
        )
    }
    SecaoMenu("Sempre")
    LinhaMenu(Icons.Outlined.Apps, "Segurar um app", "mensagens, atalhos, favoritos, renomear, esconder, desinstalar")
    LinhaMenu(Icons.Outlined.Home, "Segurar um espaço vazio", "o menu do launcher")
    LinhaMenu(Icons.Outlined.TouchApp, "Alfabeto na direita", "arrastar mostra os apps da letra; volta sozinho")
}

// ---------------------------------------------------------------- Apps

@Composable
private fun AbaApps(estado: EstadoAjustes, abrir: (String) -> Unit) {
    SecaoMenu("Apps")
    LinhaMenu(
        Icons.Outlined.PushPin, "Favoritos",
        if (estado.favoritos.isEmpty()) "nenhum fixado: a lista mostra os mais usados" else "${estado.favoritos.size} fixado(s) · reorganizar e tirar",
        fim = { Seta() },
    ) { abrir(PaginasAjustes.FAVORITOS) }
    LinhaMenu(
        Icons.Outlined.VisibilityOff, "Apps escondidos",
        if (estado.appsEscondidos.isEmpty()) "nenhum; segure um app › Esconder" else "${estado.appsEscondidos.size} escondido(s)",
        fim = { Seta() },
    ) { abrir(PaginasAjustes.ESCONDIDOS) }
    Text(
        "Para renomear um app, segure ele (na lista, no buscador ou em todos os apps) › Renomear.",
        color = TextoFraco, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------- Sistema

@Composable
private fun AbaSistema(estado: EstadoAjustes, acoes: AcoesAjustes) {
    var procurando by remember { mutableStateOf<String?>(null) }
    if (!estado.ehPadrao) {
        SecaoMenu("Tela inicial")
        LinhaMenu(Icons.Outlined.Home, "Usar como tela inicial", "o WolfLauncher ainda não é a tela inicial", fim = { Seta() }, aoTocar = acoes.definirPadrao)
    }

    SecaoMenu("Permissões")
    LinhaMenu(
        Icons.Outlined.NotificationsNone, "Notificações", "números nos apps, mensagens, Matilha e o player",
        fim = { Valor(if (estado.avisosPermitidos) "ligado" else "permitir") }, aoTocar = acoes.permitirAvisos,
    )
    if (!estado.avisosPermitidos) {
        LinhaMenu(
            Icons.Outlined.WarningAmber, "O Android disse “configuração restrita”?",
            "toque aqui, depois em ⋮ › Permitir configurações restritas, e volte",
            aoTocar = acoes.informacoesDoLauncher,
        )
    }
    LinhaMenu(
        Icons.Outlined.CalendarMonth, "Agenda", "os compromissos de hoje e o seu dia",
        fim = { Valor(if (estado.temAgenda) "ligado" else "permitir") }, aoTocar = if (estado.temAgenda) null else acoes.permitirAgenda,
    )
    LinhaMenu(
        Icons.Outlined.Contacts, "Contatos", "pessoas no buscador e nomes na Matilha",
        fim = { Valor(if (estado.temContatos) "ligado" else "permitir") }, aoTocar = if (estado.temContatos) null else acoes.permitirContatos,
    )
    LinhaMenu(
        Icons.Outlined.Lock, "Bloquear a tela", "serviço de acessibilidade (não lê nada da tela)",
        fim = { Valor(if (estado.bloqueioLigado) "ligado" else "ligar") }, aoTocar = acoes.ligarBloqueio,
    )

    SecaoMenu("Atualizações")
    LinhaMenu(Icons.Outlined.Update, "Procurar atualização agora", procurando ?: "versão instalada: ${Atualizador.versaoAtual}") {
        procurando = "procurando…"
        acoes.procurar { achou ->
            procurando = if (achou) "tem versão nova: veja o aviso no topo" else "você já está na última (${Atualizador.versaoAtual})"
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
