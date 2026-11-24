package org.wolfos.launcher.dados

import android.content.Context
import java.util.Calendar

/** Um app aberto pelo launcher hoje. */
data class Abertura(val quando: Long, val chave: String)

/**
 * O que a pessoa abre e a que horas, guardado so no celular. Serve para o "Agora" do
 * painel de apps e para o "voce costuma abrir nessa hora" do Seu dia.
 */
class Uso(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("uso", Context.MODE_PRIVATE)

    fun registrar(app: AppInfo) {
        val agora = System.currentTimeMillis()
        val h = horaDe(agora)
        val e = prefs.edit()
        e.putInt("a|$h|${app.chave}", prefs.getInt("a|$h|${app.chave}", 0) + 1)
        // as aberturas de hoje (as ultimas 40 bastam)
        val hoje = (prefs.getString("hoje", "").orEmpty().lines().filter { it.isNotBlank() } + "$agora\t${app.chave}").takeLast(40)
        e.putString("hoje", hoje.joinToString("\n"))
        e.apply()
    }

    /** Os apps mais abertos perto desta hora. */
    fun appsDaHora(hora: Int, apps: List<AppInfo>, quantos: Int): List<AppInfo> =
        apps.map { a -> a to vizinhas(hora).sumOf { h -> prefs.getInt("a|$h|${a.chave}", 0) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(quantos)
            .map { it.first }

    /**
     * A nota de cada app para agora: o quanto ele e aberto no total, com peso maior
     * para perto desta hora (a hora, a de antes e a de depois).
     */
    fun notas(hora: Int): Map<String, Double> {
        val total = HashMap<String, Double>()
        val perto = vizinhas(hora).toSet()
        for ((k, v) in prefs.all) {
            if (!k.startsWith("a|") || v !is Int) continue
            val partes = k.split('|', limit = 3)
            val chave = partes.getOrNull(2) ?: continue
            val peso = if (partes[1].toIntOrNull() in perto) 4.0 else 1.0
            total[chave] = (total[chave] ?: 0.0) + v * peso
        }
        return total
    }

    /** Quantas vezes cada app foi aberto pelo launcher, no total. */
    fun totais(): Map<String, Int> {
        val t = HashMap<String, Int>()
        for ((k, v) in prefs.all) {
            if (!k.startsWith("a|") || v !is Int) continue
            val chave = k.split('|', limit = 3).getOrNull(2) ?: continue
            t[chave] = (t[chave] ?: 0) + v
        }
        return t
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
