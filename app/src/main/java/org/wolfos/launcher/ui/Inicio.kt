package org.wolfos.launcher.ui

import androidx.compose.foundation.layout.widthIn
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
    val abrirWidgets: () -> Unit = {},
    /** Bloquear a tela (nulo: dois toques desligado). Devolve falso se falta ligar o servico. */
    val bloquear: (() -> Boolean)? = null,
    val ligarBloqueio: () -> Unit = {},
    /** Nulo quando o acesso as notificacoes ja esta ligado; senao, leva para ligar. */
    val permitirAvisos: (() -> Unit)? = null,
)


/**
 * A tela inicial: relogio, a faixa do agora, o painel de apps e a
 * barra de comando na altura do polegar. Para cima abre todos os apps; para baixo, as notificacoes; segurar
 * num espaco vazio abre o menu do launcher.
 */
@Composable
fun Inicio(
    atualizacao: EstadoAtualizacao,
    ehPadrao: Boolean,
    /** O proximo compromisso (de dia) ou o despertador (a noite). */
    faixaAgora: FaixaInfo?,
    noite: Boolean,
    acoes: AcoesInicio,
    clima: Clima? = null,
    /** O jeito do relogio grande. */
    estiloRelogio: EstiloRelogio = EstiloRelogio.UIVO,
    /** O clima de verdade (Open-Meteo), embaixo do relogio; com ele, o da notificacao some. */
    tempo: (@Composable () -> Unit)? = null,
    /** Os compromissos de hoje, embaixo do clima. */
    compromissos: (@Composable () -> Unit)? = null,
    /** O painel de apps (Agora e Favoritos), embaixo, perto do polegar. */
    painel: @Composable () -> Unit = {},
    /** Quantas notificacoes ha e de quem e a ultima (o resumo ao lado da Matilha). */
    avisosTotal: Int = 0,
    ultimoAviso: String? = null,
    /** O player de musica (ou nada). */
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
                // a lista de apps (com o alfabeto na direita) ocupa o meio
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp).clipToBounds()) { painel() }
                if (player != null) {
                    Spacer(Modifier.height(10.dp))
                    player()
                }
                Spacer(Modifier.height(12.dp))
                // embaixo: a Matilha e, ao lado, o resumo das notificacoes
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { matilha() }
                    Box(Modifier.padding(horizontal = 6.dp, vertical = 10.dp).size(width = 1.dp, height = 48.dp).background(Texto.copy(alpha = 0.2f)))
                    ResumoAvisos(avisosTotal, ultimoAviso, acoes.abrirNotificacoes, Modifier.padding(bottom = 14.dp).widthIn(max = 132.dp))
                }
                Spacer(Modifier.height(6.dp))
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
                LinhaMenu(Icons.Outlined.Settings, "Ajustes do WolfLauncher", "relógio, cores, Matilha, permissões", fim = { Seta() }) {
                    fechar(); acoes.abrirAjustes()
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
    // sem caixa: o icone, o texto e a acao sublinhada, flutuando sobre o wallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = aoTocar)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(21.dp))
        Text(
            texto,
            color = Texto.copy(alpha = 0.92f),
            fontSize = 14.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = SombraTexto),
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        when (botao) {
            null -> Unit
            "Criar" -> Icon(Icons.Outlined.Add, contentDescription = botao, tint = cor, modifier = Modifier.size(22.dp))
            else -> Text(
                botao, color = cor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, shadow = SombraTexto),
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
