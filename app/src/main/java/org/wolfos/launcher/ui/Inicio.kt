package org.wolfos.launcher.ui

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
)

/** O que esta esperando num verbo: a ultima notificacao e quantas tem. */
data class Previa(val texto: String, val total: Int)

/** O leque aberto: as letras desenhadas ate agora e onde o dedo parou. */
private data class EstadoLeque(val prefixo: String, val alternativas: List<Char>, val centro: Offset)

/**
 * A tela inicial: relogio, a faixa do agora, os verbos (o que voce quer fazer) e a
 * barra de comando na altura do polegar. Desenhar uma letra em qualquer lugar abre o
 * leque de apps; para cima abre todos os apps; para baixo, as notificacoes; segurar
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
    /** A musica de agora (aparece no Ouvir). */
    musica: Musica?,
    acoesMusica: AcoesMusica,
    acoes: AcoesInicio,
    /** A fileira da Matilha (ou nada), logo acima da barra de comando. */
    matilha: @Composable () -> Unit,
) {
    var menuEm by remember { mutableStateOf<Offset?>(null) }
    var leque by remember { mutableStateOf<EstadoLeque?>(null) }

    Box(Modifier.fillMaxSize()) {
        AreaDeDesenho(
            aoDeslizar = { dx, dy ->
                when (direcao(dx, dy)) {
                    -1 -> acoes.abrirGaveta()
                    1 -> acoes.abrirNotificacoes()
                    else -> if (dx > 0) acoes.abrirDia()
                }
            },
            aoLetra = { letras, fim -> leque = EstadoLeque(letras.first().toString(), letras.drop(1), fim) },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { menuEm = it }) },
        ) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
                Relogio(Modifier.padding(start = 10.dp, top = 28.dp))
                // a faixa do agora: primeiro o que falta para o launcher funcionar direito
                if (!ehPadrao) {
                    Spacer(Modifier.height(16.dp))
                    Faixa("⌂", "Usar o WolfLauncher como tela inicial", "Definir", acoes.definirPadrao)
                }
                faixaAgora?.let { f ->
                    Spacer(Modifier.height(if (ehPadrao) 16.dp else 8.dp))
                    Faixa(f.simbolo, f.texto, f.botao, f.acao)
                }
                textoAtualizacao(atualizacao)?.let { (simbolo, texto, botao) ->
                    Spacer(Modifier.height(if (ehPadrao) 16.dp else 8.dp))
                    Faixa(simbolo, texto, botao, acoes.atualizar)
                }
                // os verbos ficam embaixo, perto do polegar; se nao couberem, rolam
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.align(Alignment.BottomStart).verticalScroll(rememberScrollState()),
                    ) {
                        verbos.keys.forEachIndexed { i, v ->
                            LinhaVerbo(
                                verbo = v,
                                previa = previas[v],
                                icones = iconesDosVerbos[v].orEmpty(),
                                musica = musica.takeIf { v == Verbo.OUVIR },
                                indice = i,
                                acoesMusica = acoesMusica,
                                aoApp = acoes.abrirApp,
                            ) { acoes.abrirVerbo(v) }
                        }
                        if (verbos.isEmpty()) {
                            Text("Lendo seus apps…", color = TextoFraco, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                matilha()
                Spacer(Modifier.height(12.dp))
                BarraComandoFechada({ acoes.abrirComando("") })
                Text(
                    if (noite) "modo noite  ·  ︿ todos os apps  ·  → seu dia" else "desenhe uma letra  ·  ︿ todos os apps  ·  → seu dia",
                    color = TextoFraco.copy(alpha = 0.7f),
                    fontSize = 11.5.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp),
                )
            }

            // o menu de segurar num espaco vazio, aberto onde o dedo estava
            menuEm?.let { o ->
                Box(Modifier.offset { IntOffset(o.x.toInt(), o.y.toInt()) }) {
                    DropdownMenu(expanded = true, onDismissRequest = { menuEm = null }) {
                        DropdownMenuItem(text = { Text("Ajustes do WolfLauncher") }, onClick = { menuEm = null; acoes.abrirAjustes() })
                        DropdownMenuItem(text = { Text("Papel de parede") }, onClick = { menuEm = null; acoes.papelDeParede() })
                        DropdownMenuItem(text = { Text("Todos os apps") }, onClick = { menuEm = null; acoes.abrirGaveta() })
                    }
                }
            }
        }

        leque?.let { l ->
            Leque(
                apps = apps,
                prefixo = l.prefixo,
                alternativas = l.alternativas,
                centro = l.centro,
                aoAbrir = { app -> leque = null; acoes.abrirApp(app) },
                aoTrocarLetra = { c ->
                    // a letra reconhecida estava errada: troca a ultima e guarda a errada como alternativa
                    val errada = l.prefixo.last()
                    leque = l.copy(prefixo = l.prefixo.dropLast(1) + c, alternativas = l.alternativas - c + errada)
                },
                aoMaisLetras = { letras -> leque = l.copy(prefixo = l.prefixo + letras.first(), alternativas = letras.drop(1)) },
                aoVerTodos = { leque = null; acoes.abrirComando(l.prefixo.lowercase()) },
                aoFechar = { leque = null },
            )
        }
    }
}

/** Os controles da musica de agora. */
class AcoesMusica(
    val tocarOuPausar: () -> Unit,
    val proxima: () -> Unit,
    val abrir: () -> Unit,
)

/**
 * Um verbo na tela inicial. Com algo esperando, fica mais forte e mostra a previa; no
 * Ouvir, a musica de agora com a capa e os controles; nos outros, os apps que voce mais
 * usa daquele verbo (tocar abre direto). Entra com um deslize suave, um depois do outro.
 */
@Composable
private fun LinhaVerbo(
    verbo: Verbo,
    previa: Previa?,
    icones: List<AppInfo>,
    musica: Musica?,
    indice: Int,
    acoesMusica: AcoesMusica,
    aoApp: (AppInfo) -> Unit,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = aparecer.value
                translationY = (1f - aparecer.value) * 28.dp.toPx()
                scaleX = escala
                scaleY = escala
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = toque, indication = null, onClick = aoTocar)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    verbo.titulo,
                    fontSize = 36.sp,
                    fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
                    color = Texto,
                    style = TextStyle(shadow = SombraTexto),
                )
                when {
                    musica?.tocando == true -> Equalizador(cor, Modifier.padding(start = 12.dp, bottom = 4.dp))
                    previa != null -> Box(Modifier.padding(start = 10.dp, bottom = 14.dp).size(8.dp).clip(CircleShape).background(cor))
                }
            }
            val sub = when {
                musica != null -> (if (musica.tocando) "♪ " else "⏸ ") + musica.titulo + (if (musica.artista.isNotBlank()) " · " + musica.artista else "")
                previa != null -> previa.texto + if (previa.total > 1) "  ·  +${previa.total - 1}" else ""
                else -> null
            }
            if (sub != null) {
                Text(
                    sub,
                    color = cor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = SombraTexto),
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        if (musica != null) {
            ControlesMusica(musica, acoesMusica)
        } else if (icones.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (app in icones) {
                    IconeApp(app, 34.dp, Modifier.clip(RoundedCornerShape(10.dp)).clickable { aoApp(app) })
                }
            }
        }
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
private fun Relogio(modifier: Modifier = Modifier) {
    val momento = agora()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(momento.format(HORA), fontSize = 60.sp, fontWeight = FontWeight.Light, color = Texto, style = TextStyle(shadow = SombraTexto))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                momento.format(SEMANA).replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(momento.format(DATA), color = TextoFraco, fontSize = 13.sp)
        }
    }
}
