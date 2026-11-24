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

/** Uma paleta do WolfLauncher: a cor principal e a segunda, que fazem os degrades. */
enum class Paleta(val titulo: String, val primaria: Color, val secundaria: Color) {
    LOBO("Lobo", Color(0xFFF5A432), Color(0xFFFF6A5C)),
    AURORA("Aurora", Color(0xFF3DDCB0), Color(0xFF4C8DFF)),
    UIVO("Uivo", Color(0xFFB98CFF), Color(0xFFFF6BB5)),
    GELO("Gelo", Color(0xFF7FD3FF), Color(0xFFA7B4FF)),
    WALLPAPER("Do wallpaper", Color(0xFFF5A432), Color(0xFFFF6A5C)),
}

/** As cores da noite: lilas indo para azul, mais calmas. */
private val NOITE = Color(0xFFB98CFF) to Color(0xFF6C8CFF)

/** O degrade da paleta (principal -> segunda), na diagonal. */
@Composable
fun degrade(): Brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))

/** O degrade na horizontal (tracos, barras). */
@Composable
fun degradeH(): Brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))

@Composable
fun WolfTema(noite: Boolean = false, paleta: Paleta = Paleta.LOBO, conteudo: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val (primaria, secundaria) = when {
        noite -> NOITE
        paleta == Paleta.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(contexto).let { it.primary to it.tertiary }
        else -> paleta.primaria to paleta.secundaria
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = primaria,
            secondary = secundaria,
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
fun Aurora(acento: Color, escuro: Float, segunda: Color = Color(0xFF4C8DFF), modifier: Modifier = Modifier, degrade: Boolean = false, animado: Boolean = false) {
    // a aurora se mexe devagar (uma volta a cada 24 s); so redesenha, nao recompoe a tela
    val fase = if (animado) {
        val t = rememberInfiniteTransition(label = "aurora")
        t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "fase")
    } else {
        null
    }
    Canvas(modifier.fillMaxSize()) {
        if (degrade) {
            // na tela inicial o wallpaper aparece em cima e escurece embaixo, onde ficam os apps
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
        brilho(segunda, 0.14f + 0.05f * cos(f * 2), 0.95f - 0.10f * cos(f), 0.9f - 0.05f * sin(f))
        if (fase != null) {
            // um terceiro brilho, fraco, que atravessa o meio
            brilho(segunda, 0.08f, 0.5f + 0.35f * sin(f + 1.3f), 0.55f + 0.15f * cos(f * 1.5f), raio * 0.6f)
        }
    }
}
