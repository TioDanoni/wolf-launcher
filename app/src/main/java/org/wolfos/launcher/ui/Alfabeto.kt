package org.wolfos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * O alfabeto da lateral da lista de apps. Enquanto o dedo (ou o mouse) passa, as
 * letras perto dele crescem e saem para a esquerda numa onda, na cor da paleta, e uma
 * bolha com a letra acompanha o dedo; cada letra nova da um tique de vibracao. Ao
 * soltar, tudo volta com mola.
 */
@Composable
fun AlfabetoLateral(
    letras: List<String>,
    aoEscolher: (String) -> Unit,
    aoSoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cor = MaterialTheme.colorScheme.primary
    val segunda = MaterialTheme.colorScheme.secondary
    val haptico = LocalHapticFeedback.current
    val escolher by rememberUpdatedState(aoEscolher)
    val soltar by rememberUpdatedState(aoSoltar)
    val raio = with(LocalDensity.current) { 52.dp.toPx() }

    var altura by remember { mutableIntStateOf(1) }
    var dedo by remember { mutableFloatStateOf(-1f) }
    var tocando by remember { mutableStateOf(false) }
    var passando by remember { mutableStateOf(false) }
    var mostrada by remember { mutableStateOf("") }
    val ultima = remember { arrayOf<String?>(null) }

    fun letraEm(y: Float) = letras[((y / altura) * letras.size).toInt().coerceIn(0, letras.lastIndex)]
    fun escolherEm(y: Float) {
        val l = letraEm(y)
        if (l != ultima[0]) {
            ultima[0] = l
            mostrada = l
            haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            escolher(l)
        }
    }

    // a bolha segue o dedo com mola e aparece/some suave
    val bolhaY by animateFloatAsState(dedo, spring(dampingRatio = 0.75f, stiffness = 380f), label = "bolhaY")
    val bolhaA by animateFloatAsState(if (tocando) 1f else 0f, tween(if (tocando) 160 else 320), label = "bolhaA")
    val ativo = (tocando || passando) && dedo >= 0f

    Box(
        modifier
            .width(32.dp)
            .fillMaxHeight()
            .onSizeChanged { altura = it.height.coerceAtLeast(1) }
            // tocar e arrastar: escolhe a letra
            .pointerInput(letras) {
                awaitEachGesture {
                    val baixo = awaitFirstDown()
                    baixo.consume()
                    tocando = true
                    dedo = baixo.position.y
                    escolherEm(dedo)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val m = ev.changes.firstOrNull { it.id == baixo.id } ?: break
                        if (!m.pressed) break
                        dedo = m.position.y.coerceIn(0f, altura.toFloat())
                        m.consume()
                        escolherEm(dedo)
                    }
                    tocando = false
                    ultima[0] = null
                    soltar()
                }
            }
            // mouse por cima sem clicar: so a onda, para mostrar onde esta
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (tocando) continue
                        when (ev.type) {
                            PointerEventType.Enter, PointerEventType.Move -> if (ev.changes.none { it.pressed }) {
                                passando = true
                                dedo = ev.changes.first().position.y
                            }
                            PointerEventType.Exit -> passando = false
                        }
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            letras.forEachIndexed { i, l ->
                val centro = (i + 0.5f) * altura / letras.size
                val d = (centro - dedo) / raio
                val alvo = if (ativo) exp(-(d * d)) else 0f
                val p by animateFloatAsState(alvo, spring(dampingRatio = 0.55f, stiffness = 520f), label = "onda$i")
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        l,
                        fontSize = 11.sp,
                        fontWeight = if (p > 0.6f) FontWeight.Bold else FontWeight.Medium,
                        color = lerp(TextoFraco, cor, p.coerceIn(0f, 1f)),
                        modifier = Modifier.graphicsLayer {
                            val e = 1f + 1.15f * p
                            scaleX = e
                            scaleY = e
                            translationX = -30.dp.toPx() * p
                        },
                    )
                }
            }
        }
        // a bolha com a letra, a esquerda do alfabeto
        if (bolhaA > 0.01f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset((-112).dp.roundToPx(), (bolhaY - 34.dp.toPx()).roundToInt()) }
                    .graphicsLayer {
                        alpha = bolhaA
                        val e = 0.55f + 0.45f * bolhaA
                        scaleX = e
                        scaleY = e
                    }
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(cor, segunda))),
            ) {
                Text(
                    mostrada,
                    color = Fundo,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = Shadow(cor.copy(alpha = 0.4f), blurRadius = 8f)),
                )
            }
        }
    }
}
