package org.wolfos.launcher.dados

import android.content.Context
import android.content.pm.ApplicationInfo

/** O que a pessoa quer fazer. A tela inicial mostra verbos em vez de apps. */
enum class Verbo(val titulo: String) {
    CONVERSAR("Conversar"),
    TRABALHAR("Trabalhar"),
    OUVIR("Ouvir"),
    IR("Ir"),
    VER("Ver"),
    CRIAR("Criar"),
    JOGAR("Jogar"),
}

/**
 * Em que verbo um app entra sozinho: primeiro pelos nomes de pacote conhecidos (a
 * maioria dos apps nao declara categoria), depois pela categoria que o app declara.
 * A ordem importa: "youtube.music" e Ouvir antes de ser Ver.
 */
private val PISTAS: List<Pair<Verbo, List<String>>> = listOf(
    Verbo.TRABALHAR to listOf("slack", "teams", "zoom", ".meet", "calendar", "docs", "sheets", "slides",
        "drive", "office", "outlook", "notion", "trello", "linkedin", "todoist"),
    Verbo.CONVERSAR to listOf("whatsapp", "telegram", "securesms", "signal", "messaging", "mms", "dialer",
        "contacts", "discord", "messenger", "instagram", "twitter", "threads", "com.google.android.gm",
        "email", "skype", "telephony"),
    Verbo.OUVIR to listOf("spotify", "music", "deezer", "podcast", "radio", "audible", "soundcloud", "tidal"),
    Verbo.IR to listOf("maps", "waze", "uber", "taxis99", "moovit", "cabify", "bolt", "airbnb", "booking"),
    Verbo.VER to listOf("youtube", "netflix", "primevideo", "disney", "hbo", "globoplay", "twitch",
        "gallery", "photos", "tiktok", "reddit", "kindle", "news"),
    Verbo.CRIAR to listOf("camera", "keep", "notes", "notas", "canva", "capcut", "lightroom", "snapseed",
        "editor", "recorder", "gravador"),
)

fun verboAutomatico(app: AppInfo): Verbo? {
    val pacote = app.pacote.lowercase()
    PISTAS.firstOrNull { (_, pistas) -> pistas.any { it in pacote } }?.let { return it.first }
    return when (app.categoria) {
        ApplicationInfo.CATEGORY_SOCIAL -> Verbo.CONVERSAR
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> Verbo.TRABALHAR
        ApplicationInfo.CATEGORY_AUDIO -> Verbo.OUVIR
        ApplicationInfo.CATEGORY_MAPS -> Verbo.IR
        ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_NEWS -> Verbo.VER
        ApplicationInfo.CATEGORY_IMAGE -> Verbo.CRIAR
        ApplicationInfo.CATEGORY_GAME -> Verbo.JOGAR
        else -> null
    }
}

/** As escolhas da pessoa ("este app vai em Ouvir", "este nao vai em verbo nenhum"). */
class EscolhasVerbo(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("verbos", Context.MODE_PRIVATE)

    /** chave do app -> nome do verbo, ou [NENHUM]. */
    fun ler(): Map<String, String> = prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    fun gravar(chave: String, verbo: Verbo?) {
        prefs.edit().putString(chave, verbo?.name ?: NENHUM).apply()
    }

    companion object {
        const val NENHUM = "NENHUM"
    }
}

fun verboDe(app: AppInfo, escolhas: Map<String, String>): Verbo? = when (val e = escolhas[app.chave]) {
    null -> verboAutomatico(app)
    EscolhasVerbo.NENHUM -> null
    else -> Verbo.entries.firstOrNull { it.name == e } ?: verboAutomatico(app)
}

/** Os verbos que tem pelo menos um app, na ordem do enum, com os apps de cada um. */
fun agrupar(apps: List<AppInfo>, escolhas: Map<String, String>): Map<Verbo, List<AppInfo>> {
    val grupos = apps.groupBy { verboDe(it, escolhas) }
    return Verbo.entries.mapNotNull { v -> grupos[v]?.let { v to it } }.toMap()
}
