package org.wolfos.launcher.dados

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/** Uma cidade achada na busca. */
@Immutable
data class Cidade(val nome: String, val regiao: String, val lat: Double, val lon: Double) {
    val descricao: String get() = listOf(nome, regiao).filter { it.isNotBlank() }.joinToString(", ")
}

/** O tempo de agora e do dia. */
@Immutable
data class Previsao(
    val cidade: String,
    val graus: Int,
    /** Codigo do tempo da WMO (0 = ceu limpo, 61 = chuva...). */
    val codigo: Int,
    val dia: Boolean,
    val maxima: Int,
    val minima: Int,
    val chuva: Int,
    val quando: Long,
)

/**
 * O clima do topo, pelo Open-Meteo (gratuito, sem cadastro e sem chave). So vai para
 * la a posicao da cidade escolhida, nada da pessoa.
 */
object Tempo {
    private fun prefs(c: Context) = c.getSharedPreferences("tempo", Context.MODE_PRIVATE)

    fun ligado(c: Context) = prefs(c).getBoolean("ligado", true)
    fun definirLigado(c: Context, v: Boolean) = prefs(c).edit().putBoolean("ligado", v).apply()

    fun fahrenheit(c: Context) = prefs(c).getBoolean("fahrenheit", false)
    fun definirFahrenheit(c: Context, v: Boolean) = prefs(c).edit().putBoolean("fahrenheit", v).apply()

    fun cidade(c: Context): Cidade? {
        val p = prefs(c)
        val nome = p.getString("cidade", null) ?: return null
        return Cidade(nome, p.getString("regiao", "").orEmpty(), p.getFloat("lat", 0f).toDouble(), p.getFloat("lon", 0f).toDouble())
    }

    fun definirCidade(c: Context, cidade: Cidade) {
        prefs(c).edit()
            .putString("cidade", cidade.nome).putString("regiao", cidade.regiao)
            .putFloat("lat", cidade.lat.toFloat()).putFloat("lon", cidade.lon.toFloat())
            .remove("ultima")
            .apply()
    }

    /** A ultima previsao guardada (aparece na hora, antes de buscar de novo). */
    fun guardada(c: Context): Previsao? = runCatching {
        val j = JSONObject(prefs(c).getString("ultima", null) ?: return null)
        Previsao(
            j.getString("cidade"), j.getInt("graus"), j.getInt("codigo"), j.getBoolean("dia"),
            j.getInt("max"), j.getInt("min"), j.optInt("chuva"), j.getLong("quando"),
        )
    }.getOrNull()

    private fun guardar(c: Context, p: Previsao) {
        val j = JSONObject()
            .put("cidade", p.cidade).put("graus", p.graus).put("codigo", p.codigo).put("dia", p.dia)
            .put("max", p.maxima).put("min", p.minima).put("chuva", p.chuva).put("quando", p.quando)
        prefs(c).edit().putString("ultima", j.toString()).apply()
    }

    /** Procura cidades pelo nome (em portugues). */
    suspend fun buscarCidades(nome: String): List<Cidade> = withContext(Dispatchers.IO) {
        if (nome.isBlank()) return@withContext emptyList()
        runCatching {
            val url = "https://geocoding-api.open-meteo.com/v1/search?count=8&language=pt&format=json&name=" +
                URLEncoder.encode(nome.trim(), "UTF-8")
            val r = JSONObject(baixar(url)).optJSONArray("results") ?: return@runCatching emptyList()
            (0 until r.length()).map { i ->
                val o = r.getJSONObject(i)
                Cidade(
                    o.getString("name"),
                    listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    o.getDouble("latitude"),
                    o.getDouble("longitude"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Busca o tempo de agora da cidade escolhida (e guarda). */
    suspend fun atualizar(c: Context): Previsao? = withContext(Dispatchers.IO) {
        val cidade = cidade(c) ?: return@withContext null
        runCatching {
            val f = if (fahrenheit(c)) "&temperature_unit=fahrenheit" else ""
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${cidade.lat}&longitude=${cidade.lon}" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=1$f"
            val j = JSONObject(baixar(url))
            val agora = j.getJSONObject("current")
            val dia = j.getJSONObject("daily")
            Previsao(
                cidade = cidade.nome,
                graus = agora.getDouble("temperature_2m").roundToInt(),
                codigo = agora.getInt("weather_code"),
                dia = agora.optInt("is_day", 1) == 1,
                maxima = dia.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                minima = dia.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                chuva = dia.optJSONArray("precipitation_probability_max")?.optInt(0) ?: 0,
                quando = System.currentTimeMillis(),
            ).also { guardar(c, it) }
        }.getOrNull()
    }

    /** O que o codigo da WMO quer dizer, em portugues. */
    fun descricao(codigo: Int): String = when (codigo) {
        0 -> "Céu limpo"
        1 -> "Quase limpo"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        in 51..57 -> "Garoa"
        in 61..67 -> "Chuva"
        in 71..77 -> "Neve"
        in 80..82 -> "Pancadas de chuva"
        85, 86 -> "Neve"
        in 95..99 -> "Trovoadas"
        else -> "—"
    }

    private fun baixar(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "WolfLauncher")
        }
        return try {
            c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }
}

/** Um dia da previsao da semana. */
@Immutable
data class DiaPrevisto(val data: java.time.LocalDate, val codigo: Int, val maxima: Int, val minima: Int, val chuva: Int)

/** Uma hora da previsao das proximas horas. */
@Immutable
data class HoraPrevista(val hora: java.time.LocalDateTime, val graus: Int, val codigo: Int, val dia: Boolean, val chuva: Int)

/** A previsao completa: as proximas horas, os 7 dias e o nascer/por do sol de hoje. */
@Immutable
data class Semana(
    val cidade: String,
    val horas: List<HoraPrevista>,
    val dias: List<DiaPrevisto>,
    val nascer: java.time.LocalDateTime?,
    val por: java.time.LocalDateTime?,
)

/** A previsao da semana, pelo Open-Meteo (so quando a pessoa abre o clima). */
suspend fun Tempo.semana(c: Context): Semana? = withContext(Dispatchers.IO) {
    val cidade = cidade(c) ?: return@withContext null
    runCatching {
        val f = if (fahrenheit(c)) "&temperature_unit=fahrenheit" else ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${cidade.lat}&longitude=${cidade.lon}" +
            "&hourly=temperature_2m,weather_code,is_day,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&timezone=auto&forecast_days=7$f"
        val con = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "WolfLauncher")
        }
        val j = try {
            JSONObject(con.inputStream.bufferedReader().readText())
        } finally {
            con.disconnect()
        }
        val h = j.getJSONObject("hourly")
        val agora = java.time.LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
        val tempos = h.getJSONArray("time")
        val horas = (0 until tempos.length()).map { i ->
            HoraPrevista(
                java.time.LocalDateTime.parse(tempos.getString(i)),
                h.getJSONArray("temperature_2m").getDouble(i).roundToInt(),
                h.getJSONArray("weather_code").getInt(i),
                h.getJSONArray("is_day").getInt(i) == 1,
                h.optJSONArray("precipitation_probability")?.optInt(i) ?: 0,
            )
        }.filter { !it.hora.isBefore(agora) }.take(24)
        val d = j.getJSONObject("daily")
        val datas = d.getJSONArray("time")
        val dias = (0 until datas.length()).map { i ->
            DiaPrevisto(
                java.time.LocalDate.parse(datas.getString(i)),
                d.getJSONArray("weather_code").getInt(i),
                d.getJSONArray("temperature_2m_max").getDouble(i).roundToInt(),
                d.getJSONArray("temperature_2m_min").getDouble(i).roundToInt(),
                d.optJSONArray("precipitation_probability_max")?.optInt(i) ?: 0,
            )
        }
        Semana(
            cidade.nome,
            horas,
            dias,
            runCatching { java.time.LocalDateTime.parse(d.getJSONArray("sunrise").getString(0)) }.getOrNull(),
            runCatching { java.time.LocalDateTime.parse(d.getJSONArray("sunset").getString(0)) }.getOrNull(),
        )
    }.getOrNull()
}
