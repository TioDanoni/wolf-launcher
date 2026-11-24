package org.wolfos.launcher.dados

import android.content.ComponentName
import android.content.Context
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

    /** O app foi atualizado ou removido: o icone pode ter mudado. */
    fun esquecer(pacote: String) {
        cache.snapshot().keys.filter { it.startsWith("$pacote/") }.forEach(cache::remove)
    }
}
