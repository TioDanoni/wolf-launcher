package org.wolfos.launcher.dados

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wolfos.launcher.avisos.Aviso

/** Uma conversa do WhatsApp (pessoa ou grupo), vinda dos atalhos que o proprio app publica. */
@Immutable
data class Conversa(
    val id: String,
    val pacote: String,
    val nome: String,
    val grupo: Boolean,
    /** Quando o WhatsApp mexeu no atalho pela ultima vez (mais ou menos a ultima conversa). */
    val mudou: Long,
) {
    val chave: String = "$pacote|$id"
    val busca: String = semAcento(nome)
}

/** Quais conversas a Matilha mostra. */
enum class TipoMatilha(val titulo: String) {
    TODOS("Pessoas e grupos"),
    PESSOAS("Só pessoas"),
    GRUPOS("Só grupos"),
}

/**
 * As conversas recentes do WhatsApp. O WhatsApp publica um atalho para cada conversa
 * recente (os mesmos de segurar o icone e do compartilhar); o launcher padrao pode ler
 * esses atalhos e abrir direto na conversa. O id do atalho e o "jid" do WhatsApp:
 * "...@g.us" e grupo, o resto e pessoa.
 */
object Conversas {
    val PACOTES = listOf("com.whatsapp", "com.whatsapp.w4b")

    private val icones = LruCache<String, ImageBitmap>(40)

    /** So o launcher padrao pode ler atalhos de outros apps. */
    fun disponivel(contexto: Context): Boolean = runCatching {
        contexto.getSystemService(LauncherApps::class.java)?.hasShortcutHostPermission() == true
    }.getOrDefault(false)

    fun carregar(contexto: Context): List<Conversa> {
        val la = contexto.getSystemService(LauncherApps::class.java) ?: return emptyList()
        if (!disponivel(contexto)) return emptyList()
        var flags = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) flags = flags or LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
        return PACOTES.flatMap { pacote ->
            val consulta = LauncherApps.ShortcutQuery().setPackage(pacote).setQueryFlags(flags)
            runCatching { la.getShortcuts(consulta, Process.myUserHandle()) }.getOrNull().orEmpty()
                .filter { it.isEnabled && '@' in it.id }
                .map { s -> converter(s, pacote) }
        }
            .distinctBy { it.chave }
            .sortedByDescending { it.mudou }
    }

    private fun converter(s: ShortcutInfo, pacote: String) = Conversa(
        id = s.id,
        pacote = pacote,
        nome = (s.shortLabel ?: s.longLabel)?.toString()?.trim().orEmpty().ifEmpty { s.id.substringBefore('@') },
        grupo = s.id.endsWith("@g.us"),
        mudou = s.lastChangedTimestamp,
    )

    fun iconeDaMemoria(c: Conversa): ImageBitmap? = icones.get(c.chave)

    /** A foto da conversa (pessoa ou grupo), do jeito que o WhatsApp entrega. */
    suspend fun icone(contexto: Context, c: Conversa): ImageBitmap? {
        icones.get(c.chave)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val la = contexto.getSystemService(LauncherApps::class.java) ?: return@runCatching null
                val s = la.getShortcuts(
                    LauncherApps.ShortcutQuery().setPackage(c.pacote).setShortcutIds(listOf(c.id)).setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED else 0)
                    ),
                    Process.myUserHandle(),
                )?.firstOrNull() ?: return@runCatching null
                val densidade = contexto.resources.displayMetrics.densityDpi
                la.getShortcutIconDrawable(s, densidade)?.toBitmap(128, 128)?.asImageBitmap()
            }.getOrNull()
        }?.also { icones.put(c.chave, it) }
    }

    /** Abre direto na conversa. */
    fun abrir(contexto: Context, c: Conversa): Boolean = runCatching {
        contexto.getSystemService(LauncherApps::class.java)
            ?.startShortcut(c.pacote, c.id, null, null, Process.myUserHandle())
    }.isSuccess

    /** As notificacoes desta conversa (pelo atalho da notificacao ou pelo nome). */
    fun esperando(c: Conversa, avisos: List<Aviso>): List<Aviso> = avisos.filter { a ->
        a.pacote == c.pacote && (a.atalho == c.id || (a.titulo.isNotEmpty() && semAcento(a.titulo) == c.busca))
    }

    /**
     * A fileira: as fixadas primeiro (na ordem em que foram fixadas), depois as que estao
     * esperando resposta, depois as mais recentes, sempre no filtro escolhido.
     */
    fun matilha(todas: List<Conversa>, fixadas: List<String>, tipo: TipoMatilha, avisos: List<Aviso>, maximo: Int = 10): List<Conversa> {
        val porChave = todas.associateBy { it.chave }
        val doTipo = { c: Conversa ->
            when (tipo) {
                TipoMatilha.TODOS -> true
                TipoMatilha.PESSOAS -> !c.grupo
                TipoMatilha.GRUPOS -> c.grupo
            }
        }
        val fixas = fixadas.mapNotNull { porChave[it] }
        val resto = todas.filter { it.chave !in fixadas && doTipo(it) }
            .sortedWith(compareByDescending<Conversa> { esperando(it, avisos).isNotEmpty() }.thenByDescending { it.mudou })
        return (fixas + resto).take(maximo)
    }
}

/** As conversas fixadas na Matilha (pacote|id), na ordem. */
class Fixadas(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("matilha", Context.MODE_PRIVATE)

    fun ler(): List<String> = prefs.getString("fixadas", "").orEmpty().split('\n').filter { it.isNotBlank() }

    fun gravar(lista: List<String>) {
        prefs.edit().putString("fixadas", lista.distinct().joinToString("\n")).apply()
    }

    var tipo: TipoMatilha
        get() = TipoMatilha.entries.firstOrNull { it.name == prefs.getString("tipo", null) } ?: TipoMatilha.TODOS
        set(v) = prefs.edit().putString("tipo", v.name).apply()
}
