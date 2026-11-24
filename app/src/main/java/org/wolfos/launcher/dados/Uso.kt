package org.wolfos.launcher.dados

import android.content.Context
import java.util.Calendar

/** Um app aberto pelo launcher hoje. */
data class Abertura(val quando: Long, val chave: String)

/**
 * O que a pessoa abre e a que horas, guardado so no celular. Serve para os verbos
 * subirem na hora certa e para o "voce costuma abrir nessa hora" do Seu dia.
 */
class Uso(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("uso", Context.MODE_PRIVATE)

    fun registrar(app: AppInfo, verbo: Verbo?) {
        val agora = System.currentTimeMillis()
        val h = horaDe(agora)
        val e = prefs.edit()
        e.putInt("a|$h|${app.chave}", prefs.getInt("a|$h|${app.chave}", 0) + 1)
        if (verbo != null) e.putInt("v|$h|${verbo.name}", prefs.getInt("v|$h|${verbo.name}", 0) + 1)
        // as aberturas de hoje (as ultimas 40 bastam)
        val hoje = (prefs.getString("hoje", "").orEmpty().lines().filter { it.isNotBlank() } + "$agora\t${app.chave}").takeLast(40)
        e.putString("hoje", hoje.joinToString("\n"))
        e.apply()
    }

    /** Quanto cada verbo foi usado perto desta hora (a hora, a de antes e a de depois). */
    fun pontosDosVerbos(hora: Int): Map<Verbo, Int> = Verbo.entries.associateWith { v ->
        vizinhas(hora).sumOf { h -> prefs.getInt("v|$h|${v.name}", 0) }
    }

    /** Os apps mais abertos perto desta hora. */
    fun appsDaHora(hora: Int, apps: List<AppInfo>, quantos: Int): List<AppInfo> =
        apps.map { a -> a to vizinhas(hora).sumOf { h -> prefs.getInt("a|$h|${a.chave}", 0) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(quantos)
            .map { it.first }

    /** Os apps de cada verbo para mostrar ao lado dele: os mais abertos (pesando a hora de agora). */
    fun destaques(verbos: Map<Verbo, List<AppInfo>>, hora: Int, quantos: Int): Map<Verbo, List<AppInfo>> {
        val total = HashMap<String, Int>()
        val agora = HashMap<String, Int>()
        val perto = vizinhas(hora).toSet()
        for ((k, v) in prefs.all) {
            if (!k.startsWith("a|") || v !is Int) continue
            val partes = k.split('|', limit = 3)
            val chave = partes.getOrNull(2) ?: continue
            total[chave] = (total[chave] ?: 0) + v
            if (partes[1].toIntOrNull() in perto) agora[chave] = (agora[chave] ?: 0) + v
        }
        return verbos.mapValues { (_, apps) ->
            // sem historico ainda: os primeiros do verbo (ordem alfabetica) ja dao cor
            apps.sortedByDescending { (total[it.chave] ?: 0) + 3 * (agora[it.chave] ?: 0) }.take(quantos)
        }
    }

    fun hoje(): List<Abertura> {
        val inicio = inicioDoDia()
        return prefs.getString("hoje", "").orEmpty().lines().mapNotNull { l ->
            val partes = l.split('\t')
            val quando = partes.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val chave = partes.getOrNull(1) ?: return@mapNotNull null
            Abertura(quando, chave).takeIf { quando >= inicio }
        }
    }

    private fun vizinhas(hora: Int) = listOf((hora + 23) % 24, hora, (hora + 1) % 24)

    companion object {
        fun horaDe(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

        fun inicioDoDia(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
