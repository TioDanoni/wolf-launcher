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
import androidx.compose.ui.platform.LocalContext

/** As cores do WolfOS (as mesmas do Hello no PC). */
val Laranja = Color(0xFFF5A432)
val Fundo = Color(0xFF0D0F16)
val Superficie = Color(0xFF14161E)
val Texto = Color(0xFFECEEF3)
val TextoFraco = Color(0x99ECEEF3)

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
fun WolfTema(conteudo: @Composable () -> Unit) {
    val acento = corDestaque()
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
fun Aurora(acento: Color, escuro: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Fundo.copy(alpha = escuro))
        val raio = size.maxDimension * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(acento.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.08f),
                radius = raio,
            ),
            radius = raio,
            center = Offset(size.width * 0.1f, size.height * 0.08f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF4C8DFF).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.9f),
                radius = raio,
            ),
            radius = raio,
            center = Offset(size.width * 0.95f, size.height * 0.9f),
        )
    }
}
