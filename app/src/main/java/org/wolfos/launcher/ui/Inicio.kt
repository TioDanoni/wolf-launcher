package org.wolfos.launcher.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.outlined.WbCloudy
import org.wolfos.launcher.avisos.Clima
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import org.wolfos.launcher.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import org.wolfos.launcher.avisos.Musica
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Verbo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")
private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val SEMANA = DateTimeFormatter.ofPattern("EEEE", PT)
private val DATA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT)

/** Relogio que muda na virada de cada minuto (sem acordar o celular a toa). */
@Composable
fun agora(): LocalDateTime {
    val atual by produceState(LocalDateTime.now()) {
        while (true) {
            val agora = LocalDateTime.now()
            value = agora
            delay((60 - agora.second) * 1000L)
        }
    }
    return atual
}

/** O que a tela inicial pede para a tela principal fazer. */
class AcoesInicio(
    val abrirVerbo: (Verbo) -> Unit,
    /** Abre a barra de comando, ja com um texto (ou vazia). */
    val abrirComando: (String) -> Unit,
    val abrirGaveta: () -> Unit,
    val abrirNotificacoes: () -> Unit,
    val abrirAjustes: () -> Unit,
    val papelDeParede: () -> Unit,
    val atualizar: () -> Unit,
    val definirPadrao: () -> Unit,
    val abrirApp: (AppInfo) -> Unit,
    val abrirDia: () -> Unit,
    /** Segurar um verbo: o menu de editar. */
    val editarVerbo: (Verbo) -> Unit = {},
    /** Ajustes direto na pagina dos verbos. */
    val ajustesDosVerbos: () -> Unit = {},
    val abrirWidgets: () -> Unit = {},
    /** Bloquear a tela (nulo: dois toques desligado). Devolve falso se falta ligar o servico. */
    val bloquear: (() -> Boolean)? = null,
    val ligarBloqueio: () -> Unit = {},
    /** Nulo quando o acesso as notificacoes ja esta ligado; senao, leva para ligar. */
    val permitirAvisos: (() -> Unit)? = null,
)

/** O que esta esperando num verbo: a ultima notificacao e quantas tem. */
data class Previa(val texto: String, val total: Int)


/**
 * A tela inicial: relogio, a faixa do agora, os verbos (o que voce quer fazer) e a
 * barra de comando na altura do polegar. Para cima abre todos os apps; para baixo, as notificacoes; segurar
 * num espaco vazio abre o menu do launcher.
 */
@Composable
fun Inicio(
    apps: List<AppInfo>,
    verbos: Map<Verbo, List<AppInfo>>,
    atualizacao: EstadoAtualizacao,
    ehPadrao: Boolean,
    previas: Map<Verbo, Previa>,
    /** O proximo compromisso (de dia) ou o despertador (a noite). */
    faixaAgora: FaixaInfo?,
    noite: Boolean,
    /** Os apps que aparecem ao lado de cada verbo (os mais usados); vazio = so texto. */
    iconesDosVerbos: Map<Verbo, List<AppInfo>>,
    /** O nome de cada verbo (a pessoa pode ter trocado). */
    titulos: Map<Verbo, String>,
    /** A musica de agora (aparece no Ouvir). */
    musica: Musica?,
    acoesMusica: AcoesMusica,
    acoes: AcoesInicio,
    /** O tempo (vem da notificacao do app do clima), ao lado da data. */
    clima: Clima? = null,
    /** O jeito do relogio grande. */
    estiloRelogio: EstiloRelogio = EstiloRelogio.UIVO,
    /** O clima de verdade (Open-Meteo), embaixo do relogio; com ele, o da notificacao some. */
    tempo: (@Composable () -> Unit)? = null,
    /** Os compromissos de hoje, embaixo do clima. */
    compromissos: (@Composable () -> Unit)? = null,
    /** O player de musica (ou nada); com ele, o Ouvir nao repete os controles. */
    player: (@Composable () -> Unit)? = null,
    /** A fileira da Matilha (ou nada), logo acima da barra de comando. */
    matilha: @Composable () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    var pedindoBloqueio by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AreaDeGestos(
            aoDeslizar = { dx, dy ->
                when (direcao(dx, dy)) {
                    -1 -> acoes.abrirGaveta()
                    1 -> acoes.abrirNotificacoes()
                    else -> if (dx > 0) acoes.abrirDia() else acoes.abrirWidgets()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(acoes.bloquear != null) {
                    detectTapGestures(
                        onLongPress = { menuAberto = true },
                        onDoubleTap = acoes.bloquear?.let { bloquear -> { _: Offset -> if (!bloquear()) pedindoBloqueio = true } },
                    )
                },
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // tela baixa (ou fonte grande no sistema): o relogio encolhe para caber tudo
            val escalaRelogio = when {
                maxHeight < 640.dp -> 0.7f
                maxHeight < 740.dp -> 0.82f
                else -> 1f
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Marca(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
                if (estiloRelogio == EstiloRelogio.CLASSICO) {
                    Relogio(clima.takeIf { tempo == null }, Modifier.padding(start = 6.dp, top = 22.dp))
                } else {
                    RelogioGrande(estiloRelogio, Modifier.padding(start = 4.dp, top = 16.dp), escalaRelogio)
                }
                tempo?.let { Box(Modifier.padding(start = 2.dp, top = 10.dp).entrada(1)) { it() } }
                compromissos?.let { Box(Modifier.padding(top = 12.dp).entrada(2)) { it() } }
                // a faixa do agora: primeiro o que falta para o launcher funcionar direito
                Column(Modifier.padding(top = 14.dp).entrada(3), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!ehPadrao) FaixaLimpa("⌂", "Usar o WolfLauncher como tela inicial", "Definir", acoes.definirPadrao)
                    acoes.permitirAvisos?.let { permitir ->
                        FaixaLimpa("♪", "Ligue as notificações: música, mensagens e Matilha", "Ligar", permitir)
                    }
                    faixaAgora?.let { f -> FaixaLimpa(f.simbolo, f.texto, f.botao, f.acao) }
                    textoAtualizacao(atualizacao)?.let { (simbolo, texto, botao) -> FaixaLimpa(simbolo, texto, botao, acoes.atualizar) }
                }
                // os verbos ficam embaixo, perto do polegar; se nao couberem, rolam
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.align(Alignment.BottomStart).verticalScroll(rememberScrollState()),
                    ) {
                        val lista = verbos.keys.toList()
                        lista.forEachIndexed { i, v ->
                            LinhaVerbo(
                                verbo = v,
                                titulo = titulos[v] ?: v.titulo,
                                previa = previas[v],
                                icones = iconesDosVerbos[v].orEmpty(),
                                musica = musica.takeIf { v == Verbo.OUVIR },
                                controles = player == null,
                                indice = i,
                                ultima = i == lista.lastIndex,
                                acoesMusica = acoesMusica,
                                aoApp = acoes.abrirApp,
                                aoSegurar = { acoes.editarVerbo(v) },
                            ) { acoes.abrirVerbo(v) }
                        }
                        if (verbos.isEmpty()) {
                            Text("Lendo seus apps…", color = TextoFraco, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (player != null) {
                    player()
                    Spacer(Modifier.height(16.dp))
                }
                matilha()
                Spacer(Modifier.height(14.dp))
                BarraBusca(noite) { acoes.abrirComando("") }
                Spacer(Modifier.height(10.dp))
            }
            }

        }

        // dois toques sem o servico ligado: explica e leva para ligar
        if (pedindoBloqueio) {
            Folha({ pedindoBloqueio = false }) {
                Text(
                    "Bloquear com dois toques", color = Texto, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Text(
                    "O Android só deixa um app bloquear a tela por um serviço de acessibilidade. O do WolfLauncher " +
                        "não lê nada da tela: só bloqueia. Ligue “WolfLauncher” na próxima tela. Se o Android disser " +
                        "“configuração restrita”, vá em Informações do app › ⋮ › Permitir configurações restritas.",
                    color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                LinhaMenu(Icons.Outlined.Lock, "Ligar o bloqueio", "abre a acessibilidade do Android", fim = { Seta() }) {
                    pedindoBloqueio = false
                    acoes.ligarBloqueio()
                }
            }
        }

        // segurar num espaco vazio: o menu do launcher
        if (menuAberto) {
            val fechar = { menuAberto = false }
            Folha(fechar) {
                Text(
                    "WolfLauncher", color = Texto, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                SecaoMenu("Tela inicial")
                LinhaMenu(Icons.Outlined.Settings, "Ajustes do WolfLauncher", "verbos, Matilha, ritmo, permissões", fim = { Seta() }) {
                    fechar(); acoes.abrirAjustes()
                }
                LinhaMenu(Icons.Outlined.Category, "Editar verbos", "renomear, esconder, escolher os apps", fim = { Seta() }) {
                    fechar(); acoes.ajustesDosVerbos()
                }
                LinhaMenu(Icons.Outlined.Wallpaper, "Papel de parede", "a cor de destaque acompanha", fim = { Seta() }) {
                    fechar(); acoes.papelDeParede()
                }
                LinhaMenu(Icons.Outlined.Apps, "Todos os apps", "também deslizando para cima", fim = { Seta() }) {
                    fechar(); acoes.abrirGaveta()
                }
                LinhaMenu(Icons.Outlined.Today, "Seu dia", "também deslizando para a direita", fim = { Seta() }) {
                    fechar(); acoes.abrirDia()
                }
                LinhaMenu(Icons.Outlined.Widgets, "Widgets", "também deslizando para a esquerda", fim = { Seta() }) {
                    fechar(); acoes.abrirWidgets()
                }
            }
        }
    }
}

/** Os controles da musica de agora. */
class AcoesMusica(
    val tocarOuPausar: () -> Unit,
    val proxima: () -> Unit,
    val abrir: () -> Unit,
)

/** O desenho e a descricao de cada verbo. */
fun iconeDoVerbo(v: Verbo): ImageVector = when (v) {
    Verbo.CONVERSAR -> Icons.Outlined.ChatBubbleOutline
    Verbo.TRABALHAR -> Icons.Outlined.WorkOutline
    Verbo.OUVIR -> Icons.Outlined.Headphones
    Verbo.IR -> Icons.Outlined.Explore
    Verbo.VER -> Icons.Outlined.OndemandVideo
    Verbo.CRIAR -> Icons.Outlined.Brush
    Verbo.JOGAR -> Icons.Outlined.SportsEsports
}

fun descricaoDoVerbo(v: Verbo): String = when (v) {
    Verbo.CONVERSAR -> "Mensagens, chamadas e redes"
    Verbo.TRABALHAR -> "E-mail, agenda e documentos"
    Verbo.OUVIR -> "Música, podcasts e áudio"
    Verbo.IR -> "Mapas, transporte e viagens"
    Verbo.VER -> "Vídeos, filmes e séries"
    Verbo.CRIAR -> "Câmera, notas e edição"
    Verbo.JOGAR -> "Jogos"
}

/**
 * Um verbo na tela inicial: o desenho, uma divisoria, o nome com a descricao embaixo
 * e, na direita, os apps que voce mais usa dele (numa cor so; tocar abre direto). Com
 * algo esperando, a descricao vira a previa; no Ouvir, a musica de agora e os controles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaVerbo(
    verbo: Verbo,
    titulo: String,
    previa: Previa?,
    icones: List<AppInfo>,
    musica: Musica?,
    controles: Boolean,
    indice: Int,
    ultima: Boolean,
    acoesMusica: AcoesMusica,
    aoApp: (AppInfo) -> Unit,
    aoSegurar: () -> Unit,
    aoTocar: () -> Unit,
) {
    val cor = MaterialTheme.colorScheme.primary
    val ativo = previa != null || musica?.tocando == true
    val aparecer = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(indice * 45L)
        aparecer.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    val toque = remember { MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apertado) 0.97f else 1f, label = "escala")

    Column(
        Modifier.graphicsLayer {
            alpha = aparecer.value
            translationY = (1f - aparecer.value) * 28.dp.toPx()
            scaleX = escala
            scaleY = escala
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(interactionSource = toque, indication = null, onClick = aoTocar, onLongClick = aoSegurar)
                .padding(horizontal = 6.dp, vertical = 12.dp),
        ) {
            val corV = corDoVerbo(verbo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(corV.copy(alpha = if (ativo) 0.38f else 0.22f), corV.copy(alpha = 0.06f))))
                    .border(1.dp, corV.copy(alpha = if (ativo) 0.7f else 0.35f), RoundedCornerShape(14.dp)),
            ) { Icon(iconeDoVerbo(verbo), contentDescription = null, tint = corV, modifier = Modifier.size(24.dp)) }
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(34.dp).background(corV.copy(alpha = 0.35f)))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        titulo,
                        fontSize = 25.sp,
                        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
                        color = Texto,
                        style = TextStyle(shadow = SombraTexto),
                    )
                    when {
                        musica?.tocando == true -> Equalizador(corDoVerbo(verbo), Modifier.padding(start = 10.dp))
                        previa != null -> Box(Modifier.padding(start = 8.dp).size(7.dp).clip(CircleShape).background(corDoVerbo(verbo)))
                    }
                }
                val sub = when {
                    musica != null -> (if (musica.tocando) "♪ " else "⏸ ") + musica.titulo + (if (musica.artista.isNotBlank()) " · " + musica.artista else "")
                    previa != null -> previa.texto + if (previa.total > 1) "  ·  +${previa.total - 1}" else ""
                    else -> descricaoDoVerbo(verbo)
                }
                Text(
                    sub,
                    color = if (musica != null || previa != null) corDoVerbo(verbo) else Texto.copy(alpha = 0.6f),
                    fontSize = 12.5.sp,
                    fontWeight = if (musica != null || previa != null) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = SombraTexto),
                )
            }
            if (musica != null && controles) {
                ControlesMusica(musica, acoesMusica)
            } else if (icones.isNotEmpty() && (musica == null || !controles)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (app in icones) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).clickable { aoApp(app) },
                        ) { IconeApp(app, 26.dp, mono = true) }
                    }
                }
            }
        }
        if (!ultima) Box(Modifier.fillMaxWidth().height(1.dp).background(Texto.copy(alpha = 0.12f)))
    }
}

/** O nome do launcher em cima, com a marca do lobo e dois tracos. */
@Composable
private fun Marca(modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    // os dois tracos se abrem do lobo para fora quando a tela aparece
    val abre = remember { Animatable(0f) }
    LaunchedEffect(Unit) { abre.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.graphicsLayer { alpha = 0.3f + 0.7f * abre.value }) {
        Box(Modifier.width(44.dp).height(1.dp).graphicsLayer { scaleX = abre.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) }.background(Brush.horizontalGradient(listOf(Color.Transparent, cor))))
        Icon(
            painterResource(R.drawable.marca_lobo),
            contentDescription = null,
            tint = cor,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp).size(18.dp),
        )
        Text("Wolf", color = Texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(shadow = SombraTexto))
        Text("Launcher", color = Texto.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Light, style = TextStyle(shadow = SombraTexto))
        Box(
            Modifier.padding(start = 12.dp).width(44.dp).height(1.dp)
                .graphicsLayer { scaleX = abre.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.secondary, Color.Transparent))),
        )
    }
}

/** A faixa do agora, discreta: o desenho na cor de destaque, o texto e a acao. */
@Composable
private fun FaixaLimpa(simbolo: String, texto: String, botao: String?, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    val icone = when (simbolo) {
        "⏰" -> Icons.Outlined.Alarm
        "◷" -> Icons.Outlined.Event
        "⌂" -> Icons.Outlined.Home
        "!" -> Icons.Outlined.ErrorOutline
        "♪" -> Icons.Outlined.NotificationsActive
        else -> Icons.Outlined.SystemUpdate
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .border(1.dp, Brush.horizontalGradient(listOf(cor.copy(alpha = 0.45f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))), RoundedCornerShape(16.dp))
            .clickable(onClick = aoTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
        Text(
            texto,
            color = Texto.copy(alpha = 0.92f),
            fontSize = 14.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        when (botao) {
            null -> Unit
            "Criar" -> Icon(Icons.Outlined.Add, contentDescription = botao, tint = cor, modifier = Modifier.size(22.dp))
            else -> Text(botao, color = cor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A busca embaixo: a lupa na cor de destaque, uma divisoria e o convite. */
@Composable
private fun BarraBusca(noite: Boolean, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Superficie.copy(alpha = 0.82f))
            .border(1.dp, Brush.horizontalGradient(listOf(cor.copy(alpha = 0.55f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))), RoundedCornerShape(20.dp))
            .clickable(onClick = aoTocar)
            .padding(horizontal = 16.dp)
            .height(52.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
        Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(22.dp).background(Texto.copy(alpha = 0.2f)))
        Text(
            "Buscar apps, contatos ou comandos…",
            color = TextoFraco,
            fontSize = 14.5.sp,
            maxLines = 1,
        )
    }
}

/** Capa, tocar/pausar e proxima, ao lado do Ouvir. */
@Composable
private fun ControlesMusica(musica: Musica, acoes: AcoesMusica) {
    val cor = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        musica.capa?.let {
            Image(
                it, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = acoes.abrir),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(start = 8.dp).size(42.dp).clip(CircleShape).background(cor).clickable(onClick = acoes.tocarOuPausar),
        ) { Text(if (musica.tocando) "❚❚" else "▶", color = Fundo, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = acoes.proxima),
        ) { Text("⏭", color = Texto, fontSize = 18.sp, style = TextStyle(shadow = SombraTexto)) }
    }
}

/** Tres barrinhas que dancam enquanto a musica toca. */
@Composable
private fun Equalizador(cor: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val alturas = listOf(520, 380, 640).map { duracao ->
        t.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duracao, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "barra",
        )
    }
    Canvas(modifier.size(width = 20.dp, height = 18.dp)) {
        val largura = size.width / 5
        alturas.forEachIndexed { i, a ->
            val h = size.height * a.value
            drawRoundRect(
                cor,
                topLeft = Offset(i * largura * 2, size.height - h),
                size = Size(largura, h),
                cornerRadius = CornerRadius(largura / 2),
            )
        }
    }
}

@Composable
private fun Relogio(clima: Clima?, modifier: Modifier = Modifier) {
    val momento = agora()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        val pincelRelogio = Brush.verticalGradient(
            0.25f to Texto,
            1f to MaterialTheme.colorScheme.primary,
        )
        // cada numero desliza quando muda (o minuto virando)
        Row {
            momento.format(HORA).forEachIndexed { i, ch ->
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(450)) { -it / 2 } + fadeIn(tween(450))) togetherWith
                            (slideOutVertically(tween(450)) { it / 2 } + fadeOut(tween(300)))
                    },
                    label = "digito$i",
                ) { c ->
                    Text(
                        c.toString(),
                        fontSize = 66.sp,
                        fontWeight = FontWeight.Light,
                        style = TextStyle(brush = pincelRelogio, shadow = SombraTexto),
                    )
                }
            }
        }
        Box(Modifier.padding(horizontal = 18.dp).width(1.5.dp).height(56.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))))
        Column {
            Text(
                momento.format(SEMANA).replaceFirstChar { it.uppercase() },
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(brush = degradeH(), shadow = SombraTexto),
            )
            Text(momento.format(DATA), color = Texto.copy(alpha = 0.7f), fontSize = 15.sp, style = TextStyle(shadow = SombraTexto))
            if (clima != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Outlined.WbCloudy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text(
                        " ${clima.graus}°" + if (clima.descricao.isNotBlank()) " · ${clima.descricao}" else "",
                        color = Texto.copy(alpha = 0.7f), fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = SombraTexto),
                    )
                }
            }
        }
    }
}
