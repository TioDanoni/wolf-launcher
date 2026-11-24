package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Uma area que entende deslizar: ao soltar o dedo, [aoDeslizar] recebe para onde ele
 * andou (so conta se andou pelo menos uns 56 dp, para nao confundir com um toque).
 */
@Composable
fun AreaDeGestos(
    aoDeslizar: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable BoxScope.() -> Unit,
) {
    // o pointerInput vive mais que uma composicao: le sempre a acao mais nova
    val deslizar by rememberUpdatedState(aoDeslizar)
    val minimo = with(LocalDensity.current) { 56.dp.toPx() }
    val total = remember { floatArrayOf(0f, 0f) }
    Box(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { total[0] = 0f; total[1] = 0f },
                onDragEnd = { if (hypot(total[0], total[1]) >= minimo) deslizar(total[0], total[1]) },
                onDragCancel = { total[0] = 0f; total[1] = 0f },
            ) { _, d: Offset -> total[0] += d.x; total[1] += d.y }
        },
        content = conteudo,
    )
}

/** Para onde foi o deslize: -1 para cima, 1 para baixo, 0 para os lados. */
fun direcao(dx: Float, dy: Float): Int = when {
    abs(dy) <= abs(dx) -> 0
    dy < 0 -> -1
    else -> 1
}

@Composable
fun Pilula(texto: String, destaque: Boolean, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (destaque) cor.copy(alpha = 0.16f) else Texto.copy(alpha = 0.07f))
            .border(1.dp, if (destaque) cor else Texto.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = aoTocar)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(texto, color = if (destaque) cor else Texto, fontSize = 13.5.sp, fontWeight = FontWeight.Medium) }
}

/**
 * Entrada suave: o elemento aparece crescendo e subindo um pouco, com um atraso pela
 * posicao dele (um depois do outro).
 */
@Composable
fun Modifier.entrada(indice: Int, atraso: Long = 55L): Modifier {
    val v = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(indice * atraso)
        v.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 220f))
    }
    return this.graphicsLayer {
        alpha = v.value.coerceIn(0f, 1f)
        val e = 0.85f + 0.15f * v.value
        scaleX = e
        scaleY = e
        translationY = (1f - v.value) * 16.dp.toPx()
    }
}
