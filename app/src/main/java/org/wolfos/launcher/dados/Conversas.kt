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
import kotlin.math.exp
import kotlin.math.ln

/** Uma conversa do WhatsApp (pessoa ou grupo), vinda dos atalhos que o proprio app publica. */
@Immutable
data class Conversa(
    val id: String,
    val pacote: String,
    val nome: String,
    val grupo: Boolean,
    /** Quando o WhatsApp mexeu no atalho pela ultima vez (mais ou menos a ultima conversa). */
    val mudou: Long,
    /** A ordem que o proprio WhatsApp da (0 = a que ele acha mais importante). */
    val rank: Int = 99,
) {
    val chave: String = "$pacote|$id"
    val busca: String = semAcento(nome)
}

/** De onde vem a Matilha. */
enum class FonteMatilha(val titulo: String, val pacotes: List<String>) {
    WHATSAPP("WhatsApp", listOf("com.whatsapp")),
    BUSINESS("WhatsApp Business", listOf("com.whatsapp.w4b")),
    OS_DOIS("Os dois", listOf("com.whatsapp", "com.whatsapp.w4b")),
    AGENDA("Agenda do celular", emptyList()),
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
        rank = if (s.isDynamic) s.rank else 99,
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
     * Conversa que o WhatsApp entrega so com o numero ("+55 19 9...") ganha o nome da
     * agenda, procurando pelo numero do id ("5519...@s.whatsapp.net").
     */
    fun comNomes(conversas: List<Conversa>, contatos: List<Contato>): List<Conversa> {
        if (contatos.isEmpty()) return conversas
        val porFinal = HashMap<String, Contato>()
        for (c in contatos) {
            val d = c.numero.filter(Char::isDigit)
            if (d.length >= 8) porFinal.putIfAbsent(d.takeLast(8), c)
        }
        return conversas.map { c ->
            if (c.grupo || c.nome.any(Char::isLetter)) return@map c
            val digitos = c.id.substringBefore('@').filter(Char::isDigit).ifEmpty { c.nome.filter(Char::isDigit) }
            val achado = digitos.takeIf { it.length >= 8 }?.let { porFinal[it.takeLast(8)] }
            if (achado != null) c.copy(nome = achado.nome) else c
        }
    }

    /** O nome curto embaixo da foto: o primeiro nome, ou o final do numero ("…7777"). */
    fun nomeCurto(c: Conversa): String {
        if (c.nome.any(Char::isLetter)) return c.nome.trim().substringBefore(' ').ifEmpty { c.nome }
        val d = c.nome.filter(Char::isDigit).ifEmpty { c.id.substringBefore('@').filter(Char::isDigit) }
        return if (d.length >= 4) "…" + d.takeLast(4) else c.nome
    }

    /**
     * A fileira: as fixadas primeiro (na ordem em que foram fixadas), depois as que estao
     * esperando resposta, depois as mais recentes, sempre no filtro escolhido.
     */
    /** As fontes que fazem sentido neste celular (so os WhatsApps instalados, e a agenda). */
    fun fontesPossiveis(contexto: Context): List<FonteMatilha> {
        val tem = PACOTES.filter { p -> runCatching { contexto.packageManager.getPackageInfo(p, 0) }.isSuccess }
        return buildList {
            if ("com.whatsapp" in tem) add(FonteMatilha.WHATSAPP)
            if ("com.whatsapp.w4b" in tem) add(FonteMatilha.BUSINESS)
            if (tem.size == 2) add(FonteMatilha.OS_DOIS)
            add(FonteMatilha.AGENDA)
        }
    }

    /** Sem escolha: o WhatsApp pessoal, se tiver; senao o Business; senao a agenda. */
    fun fontePadrao(contexto: Context): FonteMatilha = fontesPossiveis(contexto).first()

    /**
     * A nota de cada conversa para a Matilha: quem esta esperando sobe muito; depois, o
     * quanto e recente (cai pela metade a cada ~2 dias) e o quanto e usada (aberturas
     * pela Matilha valem mais que mensagens recebidas); a ordem do WhatsApp desempata.
     */
    fun nota(c: Conversa, uso: UsoConversa?, avisos: List<Aviso>, agora: Long = System.currentTimeMillis()): Double {
        val ultima = maxOf(c.mudou, uso?.ultimaAbertura ?: 0L, uso?.ultimaMensagem ?: 0L)
        val dias = (agora - ultima).coerceAtLeast(0) / 86_400_000.0
        val recencia = exp(-dias / 3.0)
        val frequencia = ln(1.0 + (uso?.abertas ?: 0) * 2.0 + (uso?.mensagens ?: 0) * 0.4)
        val doWhatsapp = if (c.rank in 0..4) (5 - c.rank) * 0.15 else 0.0
        val espera = if (esperando(c, avisos).isNotEmpty()) 4.0 else 0.0
        return 3.0 * recencia + 1.2 * frequencia + doWhatsapp + espera
    }

    fun matilha(
        todas: List<Conversa>,
        fixadas: List<String>,
        tipo: TipoMatilha,
        avisos: List<Aviso>,
        uso: Map<String, UsoConversa> = emptyMap(),
        maximo: Int = 10,
        soFixadas: Boolean = false,
        /** Quem tem mensagem esperando vai para a frente (a mais recente primeiro). */
        organizarPorAvisos: Boolean = false,
    ): List<Conversa> {
        val lista = montarMatilha(todas, fixadas, tipo, avisos, uso, maximo, soFixadas)
        if (!organizarPorAvisos) return lista
        // quem esta esperando vem primeiro (a mensagem mais nova antes); o resto fica como estava
        val (comAviso, semAviso) = lista.partition { esperando(it, avisos).isNotEmpty() }
        return comAviso.sortedByDescending { c -> esperando(c, avisos).maxOf { it.quando } } + semAviso
    }

    /** Quantas mensagens a conversa tem esperando. */
    fun naoLidas(c: Conversa, avisos: List<Aviso>): Int = esperando(c, avisos).sumOf { it.mensagens }

    private fun montarMatilha(
        todas: List<Conversa>,
        fixadas: List<String>,
        tipo: TipoMatilha,
        avisos: List<Aviso>,
        uso: Map<String, UsoConversa>,
        maximo: Int,
        soFixadas: Boolean,
    ): List<Conversa> {
        val porChave = todas.associateBy { it.chave }
        val doTipo = { c: Conversa ->
            when (tipo) {
                TipoMatilha.TODOS -> true
                TipoMatilha.PESSOAS -> !c.grupo
                TipoMatilha.GRUPOS -> c.grupo
            }
        }
        val fixas = fixadas.mapNotNull { porChave[it] }
        if (soFixadas) return fixas.take(maximo)
        val resto = todas.filter { it.chave !in fixadas && doTipo(it) }
            .sortedByDescending { nota(it, uso[it.chave], avisos) }
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

    /** Nulo: a pessoa ainda nao escolheu (o launcher decide pelo que esta instalado). */
    var fonte: FonteMatilha?
        get() = FonteMatilha.entries.firstOrNull { it.name == prefs.getString("fonte", null) }
        set(v) = prefs.edit().putString("fonte", v?.name).apply()
}
