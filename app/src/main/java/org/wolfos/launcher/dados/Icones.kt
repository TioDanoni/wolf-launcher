package org.wolfos.launcher.dados

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Os icones dos apps, carregados so quando aparecem na tela e guardados num cache
 * pequeno. Assim a lista abre na hora e nao segura centenas de imagens na memoria.
 */
object Icones {
    private const val TAMANHO = 112
    private val cache = LruCache<String, ImageBitmap>(240)

    fun daMemoria(app: AppInfo): ImageBitmap? = cache.get(app.chave)

    suspend fun carregar(contexto: Context, app: AppInfo): ImageBitmap? {
        cache.get(app.chave)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                contexto.packageManager.getActivityIcon(ComponentName(app.pacote, app.atividade))
                    .toBitmap(TAMANHO, TAMANHO).asImageBitmap()
            }.getOrNull()
        }?.also { cache.put(app.chave, it) }
    }

    fun monoDaMemoria(app: AppInfo): ImageBitmap? = cache.get("m:" + app.chave)

    /**
     * O icone numa cor so (branco, para pintar): a camada monocromatica que o app
     * entrega para os icones tematicos (Android 13+); sem ela, o icone em tons de cinza.
     */
    suspend fun carregarMono(contexto: Context, app: AppInfo): ImageBitmap? {
        cache.get("m:" + app.chave)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val d = contexto.packageManager.getActivityIcon(ComponentName(app.pacote, app.atividade))
                // mutate: o Android guarda os icones em cache, e o filtro nao pode vazar para o icone colorido
                val mono = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (d as? AdaptiveIconDrawable)?.monochrome?.mutate() else null
                val bmp = Bitmap.createBitmap(TAMANHO, TAMANHO, Bitmap.Config.ARGB_8888)
                val tela = Canvas(bmp)
                if (mono != null) {
                    // a camada tem 108 unidades e so as 72 do meio aparecem: estica 25% para cada lado
                    val sobra = TAMANHO / 4
                    mono.setBounds(-sobra, -sobra, TAMANHO + sobra, TAMANHO + sobra)
                    mono.colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
                    mono.draw(tela)
                } else {
                    val c = d.mutate()
                    c.setBounds(0, 0, TAMANHO, TAMANHO)
                    c.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    c.draw(tela)
                }
                bmp.asImageBitmap()
            }.getOrNull()
        }?.also { cache.put("m:" + app.chave, it) }
    }

    /** O app foi atualizado ou removido: o icone pode ter mudado. */
    fun esquecer(pacote: String) {
        cache.snapshot().keys.filter { it.removePrefix("m:").startsWith("$pacote/") }.forEach(cache::remove)
    }
}
