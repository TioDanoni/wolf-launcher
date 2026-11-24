package org.wolfos.launcher.dados

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * As cores do wallpaper, para o launcher se moldar a ele. O Android ja calcula as
 * cores principais da imagem (sem permissao, Android 8.1+); daqui sai a mais viva para
 * ser a cor principal e uma segunda para os degrades, com o brilho ajustado para ler
 * bem sobre o fundo escuro. Acompanha sozinho quando o wallpaper muda.
 */
object CoresWallpaper {
    private val _cores = MutableStateFlow<Pair<Color, Color>?>(null)
    val cores: StateFlow<Pair<Color, Color>?> = _cores.asStateFlow()

    private var ouvindo = false

    fun iniciar(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        val wm = WallpaperManager.getInstance(contexto) ?: return
        atualizar(runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull())
        if (!ouvindo) {
            ouvindo = true
            runCatching {
                wm.addOnColorsChangedListener({ c, qual ->
                    if (qual and WallpaperManager.FLAG_SYSTEM != 0) atualizar(c)
                }, Handler(Looper.getMainLooper()))
            }
        }
    }

    private fun atualizar(c: WallpaperColors?) {
        if (c == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        val todas = listOfNotNull(c.primaryColor, c.secondaryColor, c.tertiaryColor).map { it.toArgb() }
        if (todas.isEmpty()) return
        _cores.value = escolher(todas)
    }

    /** A cor principal e a segunda, a partir das cores do wallpaper. */
    fun escolher(cores: List<Int>): Pair<Color, Color> {
        val hsl = cores.map { FloatArray(3).also { h -> ColorUtils.colorToHSL(it, h) } }
        // a principal e a mais viva (saturada); wallpaper quase sem cor fica num tom claro e calmo
        val principal = hsl.maxBy { it[1] * (1f - abs(it[2] - 0.5f)) }
        // a segunda: outra cor com matiz diferente; sem ela, a principal girada 35 graus
        val segunda = hsl.filter { it !== principal && it[1] > 0.15f && distanciaMatiz(it[0], principal[0]) > 20f }
            .maxByOrNull { it[1] }
            ?: floatArrayOf((principal[0] + 35f) % 360f, principal[1], principal[2])
        return paraTelaEscura(principal) to paraTelaEscura(segunda)
    }

    /** Viva e clara o bastante para ler sobre o fundo escuro, sem estourar. */
    private fun paraTelaEscura(h: FloatArray): Color {
        val s = if (h[1] < 0.12f) h[1] else h[1].coerceIn(0.55f, 0.9f)
        val l = h[2].coerceIn(0.62f, 0.74f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(h[0], s, l)))
    }

    private fun distanciaMatiz(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }
}
