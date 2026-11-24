package org.wolfos.launcher.ui

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import org.wolfos.launcher.dados.Gesto
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
    /** Faz o que o gesto esta configurado para fazer; falso se faltar ligar o bloqueio. */
    val gesto: (Gesto) -> Boolean = { true },
    /** O "tocar duas vezes" faz alguma coisa (senao, nem espera o segundo toque). */
    val doisToquesAtivo: Boolean = true,
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
    /** A marca "WolfLauncher" no topo. */
    mostrarMarca: Boolean = true,
    /** O player de musica (ou nada). */
    player: (@Composable () -> Unit)? = null,
    /** A fileira da Matilha (ou nada), logo acima da barra de comando. */
    matilha: @Composable () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    var pedindoBloqueio by remember { mutableStateOf(false) }
    var puxaoAlvo by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val puxao by animateFloatAsState(puxaoAlvo, androidx.compose.animation.core.spring(stiffness = 1400f), label = "puxao")
    val acoesAtuais by androidx.compose.runtime.rememberUpdatedState(acoes)
    val limitePuxao = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
    val conexaoGestos = remember {
        object : NestedScrollConnection {
            var sobra = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    sobra += available.y
                    when {
                        sobra < -limitePuxao -> { sobra = 0f; acoesAtuais.gesto(Gesto.CIMA) }
                        sobra > limitePuxao -> { sobra = 0f; acoesAtuais.gesto(Gesto.BAIXO) }
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                sobra = 0f
                when {
                    available.y < -2200f -> acoesAtuais.gesto(Gesto.CIMA)
                    available.y > 2200f -> acoesAtuais.gesto(Gesto.BAIXO)
                }
                return Velocity.Zero
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AreaDeGestos(
            // a tela acompanha o dedo puxando para cima (resposta na hora, antes de soltar)
            aoArrastar = { dx, dy -> puxaoAlvo = if (dy < 0 && kotlin.math.abs(dy) > kotlin.math.abs(dx)) dy else 0f },
            aoDeslizar = { dx, dy ->
                // cada gesto faz o que a pessoa escolheu nos ajustes
                when (direcao(dx, dy)) {
                    -1 -> acoes.gesto(Gesto.CIMA)
                    1 -> acoes.gesto(Gesto.BAIXO)
                    else -> acoes.gesto(if (dx > 0) Gesto.DIREITA else Gesto.ESQUERDA)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                // quando a lista rola e chega no fim, continuar puxando tambem abre (e o flick conta)
                .nestedScroll(conexaoGestos)
                .pointerInput(acoes.doisToquesAtivo) {
                    detectTapGestures(
                        onLongPress = { menuAberto = true },
                        // sem nada no "tocar duas vezes", nao espera o segundo toque (o toque fica mais rapido)
                        onDoubleTap = if (acoes.doisToquesAtivo) ({ _: Offset -> if (!acoes.gesto(Gesto.DOIS_TOQUES)) pedindoBloqueio = true }) else null,
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
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (puxao * 0.35f).coerceAtLeast(-140.dp.toPx())
                        alpha = 1f - (kotlin.math.abs(puxao) / 1400f).coerceAtMost(0.45f)
                    }
                    .padding(horizontal = 18.dp),
            ) {
                if (mostrarMarca) Marca(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
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
                // embaixo: a Matilha
                matilha()
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
                LinhaMenu(Icons.Outlined.Apps, "Buscar e todos os apps", "também deslizando para cima", fim = { Seta() }) {
                    fechar(); acoes.abrirGaveta()
                }
                LinhaMenu(Icons.Outlined.Today, "Seu dia", "também deslizando para a direita", fim = { Seta() }) {
                    fechar(); acoes.abrirDia()
                }
                LinhaMenu(Icons.Outlined.Widgets, "Painel", "ferramentas, notas, timer e widgets", fim = { Seta() }) {
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
