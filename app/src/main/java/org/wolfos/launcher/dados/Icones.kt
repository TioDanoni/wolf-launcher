package org.wolfos.launcher.dados

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.LruCache
import android.util.Xml
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.Collections

/** Um pacote de icones instalado (padrao Nova/ADW, o da maioria na Play Store). */
data class PacoteIcones(val pacote: String, val nome: String)

/**
 * Os icones dos apps, carregados so quando aparecem na tela e guardados num cache
 * pequeno. O estilo vale para o launcher inteiro:
 * - ORIGINAL: o icone colorido do app;
 * - LINHA: o icone tematico do Android 13+ em branco (fino, numa cor so);
 * - PALETA: o mesmo, pintado na cor da paleta na hora de desenhar;
 * - CINZA: o icone do app sem cor, suave;
 * - "pack:<pacote>": o desenho de um pacote de icones instalado.
 */
object Icones {
    private const val TAMANHO = 112
    private val cache = LruCache<String, ImageBitmap>(300)

    /** Os apps sem icone tematico (no LINHA/PALETA viram cinza e nao recebem a cor). */
    private val semTema: MutableSet<String> = Collections.synchronizedSet(HashSet())

    @Volatile
    var estilo: String = "ORIGINAL"
        private set

    /** Muda quando o estilo muda: a tela redesenha os icones. */
    val versao = mutableIntStateOf(0)

    fun definirEstilo(novo: String) {
        if (novo == estilo) return
        estilo = novo
        cache.evictAll()
        semTema.clear()
        mapaDoPacote = null
        versao.intValue++
    }

    fun daMemoria(app: AppInfo): ImageBitmap? = cache.get(app.chave)

    /** Pintar este icone com a cor da paleta (so no estilo PALETA, e so os que tem tema). */
    fun tingir(app: AppInfo): Boolean = estilo == "PALETA" && app.chave !in semTema

    suspend fun carregar(contexto: Context, app: AppInfo): ImageBitmap? {
        cache.get(app.chave)?.let { return it }
        val e = estilo
        return withContext(Dispatchers.IO) {
            runCatching {
                when {
                    e == "LINHA" || e == "PALETA" -> emLinha(contexto, app)
                    e == "CINZA" -> cinza(contexto, app)
                    e.startsWith("pack:") -> doPacote(contexto, app, e.removePrefix("pack:")) ?: original(contexto, app)
                    else -> original(contexto, app)
                }
            }.getOrNull()
        }?.also { if (e == estilo) cache.put(app.chave, it) }
    }

    private fun original(contexto: Context, app: AppInfo): ImageBitmap =
        contexto.packageManager.getActivityIcon(ComponentName(app.pacote, app.atividade)).toBitmap(TAMANHO, TAMANHO).asImageBitmap()

    /**
     * O icone numa cor so (branco): a camada monocromatica que o app entrega para os
     * icones tematicos (Android 13+); sem ela, o icone em tons de cinza.
     */
    private fun emLinha(contexto: Context, app: AppInfo): ImageBitmap {
        val d = contexto.packageManager.getActivityIcon(ComponentName(app.pacote, app.atividade))
        // mutate: o Android guarda os icones em cache, e o filtro nao pode vazar para o colorido
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
            semTema += app.chave
            desenharSemCor(d, tela, 1f)
        }
        return bmp.asImageBitmap()
    }

    private fun cinza(contexto: Context, app: AppInfo): ImageBitmap {
        val d = contexto.packageManager.getActivityIcon(ComponentName(app.pacote, app.atividade))
        val bmp = Bitmap.createBitmap(TAMANHO, TAMANHO, Bitmap.Config.ARGB_8888)
        desenharSemCor(d, Canvas(bmp), 0.82f)
        return bmp.asImageBitmap()
    }

    /** O icone sem cor (e um pouco mais claro/suave quando [forca] < 1). */
    private fun desenharSemCor(d: Drawable, tela: Canvas, forca: Float) {
        val c = d.mutate()
        c.setBounds(0, 0, TAMANHO, TAMANHO)
        val m = ColorMatrix().apply { setSaturation(0f) }
        if (forca < 1f) {
            // menos contraste e um pouco mais claro: fica "suave" sobre o wallpaper
            val k = forca
            val desloca = (1f - k) * 128f
            m.postConcat(ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, desloca, 0f, k, 0f, 0f, desloca, 0f, 0f, k, 0f, desloca, 0f, 0f, 0f, 1f, 0f)))
        }
        c.colorFilter = ColorMatrixColorFilter(m)
        c.draw(tela)
        c.colorFilter = null
    }

    // ---------------------------------------------------------------- pacotes de icones

    private val ACOES_PACOTE = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.gau.go.launcherex.theme")

    /** Os pacotes de icones instalados. */
    fun pacotesInstalados(contexto: Context): List<PacoteIcones> {
        val pm = contexto.packageManager
        return ACOES_PACOTE.flatMap { acao ->
            runCatching { pm.queryIntentActivities(Intent(acao), PackageManager.GET_META_DATA) }.getOrDefault(emptyList())
        }
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { p ->
                runCatching { PacoteIcones(p, pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString()) }.getOrNull()
            }
            .sortedBy { it.nome.lowercase() }
    }

    /** O pacote aberto agora: os recursos dele e o mapa "pacote/atividade" -> nome do desenho. */
    private var mapaDoPacote: Pair<Resources, Map<String, String>>? = null

    private fun doPacote(contexto: Context, app: AppInfo, pacote: String): ImageBitmap? {
        val (res, mapa) = mapaDoPacote ?: abrirPacote(contexto, pacote)?.also { mapaDoPacote = it } ?: return null
        val nome = mapa[app.chave] ?: return null
        val id = res.getIdentifier(nome, "drawable", pacote).takeIf { it != 0 } ?: return null
        @Suppress("DEPRECATION")
        val d = runCatching { res.getDrawableForDensity(id, DisplayMetrics.DENSITY_XXXHIGH, null) }.getOrNull()
            ?: res.getDrawable(id, null)
        return d.toBitmap(TAMANHO, TAMANHO).asImageBitmap()
    }

    /** Le o appfilter.xml do pacote (em res/xml ou em assets). */
    private fun abrirPacote(contexto: Context, pacote: String): Pair<Resources, Map<String, String>>? = runCatching {
        val res = contexto.packageManager.getResourcesForApplication(pacote)
        val id = res.getIdentifier("appfilter", "xml", pacote)
        val leitor: XmlPullParser = if (id != 0) {
            res.getXml(id)
        } else {
            Xml.newPullParser().apply { setInput(res.assets.open("appfilter.xml"), "UTF-8") }
        }
        val mapa = HashMap<String, String>()
        var ev = leitor.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && leitor.name == "item") {
                val componente = leitor.getAttributeValue(null, "component")
                val desenho = leitor.getAttributeValue(null, "drawable")
                if (componente != null && !desenho.isNullOrBlank()) {
                    // "ComponentInfo{com.whatsapp/com.whatsapp.Main}" -> "com.whatsapp/com.whatsapp.Main"
                    val dentro = componente.substringAfter('{', "").substringBefore('}')
                    val p = dentro.substringBefore('/')
                    var a = dentro.substringAfter('/', "")
                    if (a.startsWith(".")) a = p + a
                    if (p.isNotEmpty() && a.isNotEmpty()) mapa.putIfAbsent("$p/$a", desenho)
                }
            }
            ev = leitor.next()
        }
        res to mapa
    }.getOrNull()

    /** O app foi atualizado ou removido: o icone pode ter mudado. */
    fun esquecer(pacote: String) {
        cache.snapshot().keys.filter { it.startsWith("$pacote/") }.forEach(cache::remove)
        if (estilo == "pack:$pacote") mapaDoPacote = null
    }
}
