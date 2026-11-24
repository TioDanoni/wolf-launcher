package org.wolfos.launcher.dados

import android.content.Context

/** Os apps fixados no painel da tela inicial (chaves pacote/atividade), na ordem. */
class Favoritos(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("favoritos", Context.MODE_PRIVATE)

    fun ler(): List<String> = prefs.getString("lista", "").orEmpty().split('\n').filter { it.isNotBlank() }

    fun gravar(lista: List<String>) {
        prefs.edit().putString("lista", lista.distinct().take(MAXIMO).joinToString("\n")).apply()
    }

    companion object {
        const val MAXIMO = 8
    }
}

/**
 * O "Agora" do painel: os apps que fazem sentido neste momento. Pesa o costume da
 * hora, quem tem notificacao esperando e o app que esta tocando musica; deixa de
 * fora os que ja estao nos favoritos.
 */
object Sugestoes {
    fun agora(
        apps: List<AppInfo>,
        notas: Map<String, Double>,
        avisosPorPacote: Map<String, Int>,
        tocandoPacote: String?,
        fora: Set<String>,
        quantos: Int = 4,
    ): List<AppInfo> = apps
        .filter { it.chave !in fora }
        .map { a ->
            var n = notas[a.chave] ?: 0.0
            if ((avisosPorPacote[a.pacote] ?: 0) > 0) n += 6.0
            if (a.pacote == tocandoPacote) n += 50.0
            a to n
        }
        .filter { it.second > 0.0 }
        .sortedByDescending { it.second }
        .take(quantos)
        .map { it.first }

    /** Sem historico ainda: apps comuns que quase todo mundo usa, para o painel nao nascer vazio. */
    private val COMUNS = listOf("whatsapp", "chrome", "camera", "dialer", "youtube", "maps", "photos", "gallery", "instagram", "spotify", "music", "gm")

    fun favoritosPadrao(apps: List<AppInfo>, notas: Map<String, Double>, quantos: Int = 4): List<AppInfo> {
        val usados = apps.filter { (notas[it.chave] ?: 0.0) > 0.0 }.sortedByDescending { notas[it.chave] }
        val comuns = COMUNS.mapNotNull { p -> apps.firstOrNull { p in it.pacote.lowercase() } }
        return (usados + comuns).distinctBy { it.chave }.take(quantos)
    }
}
