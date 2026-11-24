package org.wolfos.launcher.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext

/** As cores do WolfOS (as mesmas do Hello no PC). */
val Laranja = Color(0xFFF5A432)
val Fundo = Color(0xFF0D0F16)
val Superficie = Color(0xFF14161E)
val Texto = Color(0xFFECEEF3)
val TextoFraco = Color(0x99ECEEF3)

/** Sombra leve para o texto ler bem sobre qualquer wallpaper. */
val SombraTexto = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(0f, 2f), 10f)

/** A cor do modo noite: mais calma que o laranja. */
val Lilas = Color(0xFFB98CFF)

/**
 * A cor de destaque: no Android 12 ou mais novo vem do wallpaper (como o Noctalia
 * faz no PC); antes disso, o laranja do WolfOS.
 */
@Composable
fun corDestaque(): Color {
    val contexto = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(contexto).primary else Laranja
}

@Composable
fun WolfTema(noite: Boolean = false, conteudo: @Composable () -> Unit) {
    val acento = if (noite) Lilas else corDestaque()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = acento,
            background = Fundo,
            surface = Superficie,
            surfaceContainer = Superficie,
            onBackground = Texto,
            onSurface = Texto,
        ),
        content = conteudo,
    )
}

/**
 * O fundo: o wallpaper do celular escurecido e dois brilhos da cor de destaque (a
 * aurora do Hello). Parado de proposito: animar o fundo gastaria bateria a toa.
 */
@Composable
fun Aurora(acento: Color, escuro: Float, modifier: Modifier = Modifier, degrade: Boolean = false, animado: Boolean = false) {
    // a aurora se mexe devagar (uma volta a cada 24 s); so redesenha, nao recompoe a tela
    val fase = if (animado) {
        val t = rememberInfiniteTransition(label = "aurora")
        t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "fase")
    } else {
        null
    }
    Canvas(modifier.fillMaxSize()) {
        if (degrade) {
            // na tela inicial o wallpaper aparece em cima e escurece embaixo, onde ficam os verbos
            drawRect(
                Brush.verticalGradient(
                    0f to Fundo.copy(alpha = escuro * 0.3f),
                    0.45f to Fundo.copy(alpha = escuro * 0.75f),
                    1f to Fundo.copy(alpha = (escuro * 2f).coerceAtMost(0.92f)),
                )
            )
        } else {
            drawRect(Fundo.copy(alpha = escuro))
        }
        val f = fase?.value ?: 0f
        val raio = size.maxDimension * 0.75f
        fun brilho(cor: Color, alfa: Float, x: Float, y: Float, r: Float = raio) {
            val c = Offset(size.width * x, size.height * y)
            drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = alfa), Color.Transparent), center = c, radius = r), radius = r, center = c)
        }
        brilho(acento, 0.20f + 0.06f * sin(f * 2), 0.1f + 0.10f * sin(f), 0.08f + 0.06f * cos(f))
        brilho(Color(0xFF4C8DFF), 0.12f + 0.04f * cos(f * 2), 0.95f - 0.10f * cos(f), 0.9f - 0.05f * sin(f))
        if (fase != null) {
            // um terceiro brilho, fraco, que atravessa o meio
            brilho(acento, 0.07f, 0.5f + 0.35f * sin(f + 1.3f), 0.55f + 0.15f * cos(f * 1.5f), raio * 0.6f)
        }
    }
}
