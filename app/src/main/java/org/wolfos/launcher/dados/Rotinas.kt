package org.wolfos.launcher.dados

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import java.time.LocalDateTime

/** Os contextos do dia. Cada rotina sobe os apps dela nas sugestoes. */
enum class Rotina(val titulo: String, private val pistas: List<String>) {
    TRABALHO("Trabalho", listOf("gm", "outlook", "teams", "slack", "calendar", "drive", "docs", "sheets", ".meet", "zoom", "notion", "trello")),
    CASA("Casa", listOf("youtube", "netflix", "primevideo", "disney", "ifood", "smartthings", "home", "globoplay")),
    ACADEMIA("Academia", listOf("spotify", "music", "strava", "fit", "health", "saude", "treino", "tune")),
    VIAGEM("Viagem", listOf("maps", "waze", "uber", "taxis99", "booking", "airbnb", "latam", "voegol", "voeazul", "translate")),
    DESCANSO("Descanso", listOf("kindle", "books", "music", "spotify", "calm", "sleep", "podcast"));

    /** Os apps que entram na rotina sem a pessoa escolher (pelo nome do pacote). */
    fun appsPadrao(apps: List<AppInfo>): List<AppInfo> =
        apps.filter { a -> pistas.any { it in a.pacote.lowercase() } }.take(8)
}

/**
 * As rotinas: cada uma pode ligar sozinha por dia da semana e horario, ou ser ligada a
 * mao (pela busca, "modo trabalho", ou pelos ajustes). So uma fica ativa por vez; a
 * ligada a mao vale ate desligar.
 */
class Rotinas(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("rotinas", Context.MODE_PRIVATE)

    fun automatica(r: Rotina): Boolean = prefs.getBoolean("auto_${r.name}", false)
    fun definirAutomatica(r: Rotina, v: Boolean) = prefs.edit().putBoolean("auto_${r.name}", v).apply()

    /** Dias da semana (1 = segunda ... 7 = domingo). */
    fun dias(r: Rotina): Set<Int> = prefs.getString("dias_${r.name}", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
        ?: if (r == Rotina.TRABALHO) (1..5).toSet() else (1..7).toSet()

    fun definirDias(r: Rotina, d: Set<Int>) = prefs.edit().putString("dias_${r.name}", d.sorted().joinToString(",")).apply()

    /** Horario (minutos do dia); pode virar a meia-noite. */
    fun janela(r: Rotina): Pair<Int, Int> = prefs.getInt("ini_${r.name}", padraoIni(r)) to prefs.getInt("fim_${r.name}", padraoFim(r))
    fun definirJanela(r: Rotina, ini: Int, fim: Int) = prefs.edit().putInt("ini_${r.name}", ini).putInt("fim_${r.name}", fim).apply()

    private fun padraoIni(r: Rotina) = when (r) {
        Rotina.TRABALHO -> 9 * 60
        Rotina.ACADEMIA -> 18 * 60
        Rotina.CASA -> 19 * 60
        Rotina.VIAGEM -> 8 * 60
        Rotina.DESCANSO -> 22 * 60
    }

    private fun padraoFim(r: Rotina) = when (r) {
        Rotina.TRABALHO -> 18 * 60
        Rotina.ACADEMIA -> 19 * 60 + 30
        Rotina.CASA -> 22 * 60
        Rotina.VIAGEM -> 20 * 60
        Rotina.DESCANSO -> 7 * 60
    }

    /** Os apps escolhidos para a rotina (vazio = os padrao, pelo nome). */
    fun apps(r: Rotina): List<String> = prefs.getString("apps_${r.name}", "").orEmpty().split('\n').filter { it.isNotBlank() }
    fun definirApps(r: Rotina, l: List<String>) = prefs.edit().putString("apps_${r.name}", l.distinct().joinToString("\n")).apply()

    /** A rotina ligada a mao (ou nula). */
    var manual: Rotina?
        get() = Rotina.entries.firstOrNull { it.name == prefs.getString("manual", null) }
        set(v) = prefs.edit().putString("manual", v?.name).apply()

    /** A rotina valendo agora: a ligada a mao; senao a primeira automatica no horario. */
    fun ativa(agora: LocalDateTime = LocalDateTime.now()): Rotina? {
        manual?.let { return it }
        val dia = agora.dayOfWeek.value
        val minuto = agora.hour * 60 + agora.minute
        return Rotina.entries.firstOrNull { r ->
            automatica(r) && dia in dias(r) && janela(r).let { (a, b) -> if (a <= b) minuto in a until b else minuto >= a || minuto < b }
        }
    }
}

/** O Modo Caca (foco): esconde as distracoes da tela inicial por um tempo. */
class ModoCaca(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("caca", Context.MODE_PRIVATE)

    /** Quando o foco acaba (0 = desligado). */
    val ate: Long get() = prefs.getLong("ate", 0L).takeIf { it > System.currentTimeMillis() } ?: 0L
    val ativo: Boolean get() = ate > 0

    fun comecar(minutos: Int) = prefs.edit().putLong("ate", System.currentTimeMillis() + minutos * 60_000L).apply()
    fun parar() = prefs.edit().putLong("ate", 0L).apply()

    /** Apps que a pessoa marcou como distracao (alem dos automaticos). */
    fun extras(): Set<String> = prefs.getStringSet("extras", emptySet()).orEmpty().toSet()
    fun alternarExtra(chave: String) {
        val e = extras()
        prefs.edit().putStringSet("extras", if (chave in e) e - chave else e + chave).apply()
    }

    /** Apps que a pessoa disse que NAO sao distracao (vence o automatico). */
    fun liberados(): Set<String> = prefs.getStringSet("liberados", emptySet()).orEmpty().toSet()
    fun alternarLiberado(chave: String) {
        val e = liberados()
        prefs.edit().putStringSet("liberados", if (chave in e) e - chave else e + chave).apply()
    }

    fun ehDistracao(app: AppInfo): Boolean {
        if (app.chave in liberados()) return false
        if (app.chave in extras()) return true
        val p = app.pacote.lowercase()
        return DISTRACOES.any { it in p } || app.categoria == android.content.pm.ApplicationInfo.CATEGORY_GAME
    }

    companion object {
        private val DISTRACOES = listOf(
            "instagram", "tiktok", "musically", "youtube", "facebook", "twitter", "reddit", "kwai", "snapchat",
            "pinterest", "netflix", "twitch", "threads", "9gag", "shopee", "aliexpress",
        )
    }
}

/** Uma chamada perdida de hoje. */
@Immutable
data class Perdida(val nome: String, val numero: String, val quando: Long) {
    /** Os 8 ultimos digitos (para achar a pessoa na agenda e na Matilha). */
    val final8: String = numero.filter(Char::isDigit).takeLast(8)
}

/** As chamadas perdidas de hoje (com a permissao do registro de chamadas). */
object Chamadas {
    fun temPermissao(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun perdidasHoje(c: Context): List<Perdida> {
        if (!temPermissao(c)) return emptyList()
        val desde = Uso.inicioDoDia()
        val lista = mutableListOf<Perdida>()
        runCatching {
            c.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} >= ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), desde.toString()),
                "${CallLog.Calls.DATE} DESC",
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val numero = cur.getString(1) ?: continue
                    lista += Perdida(cur.getString(0)?.takeIf { it.isNotBlank() } ?: numero, numero, cur.getLong(2))
                }
            }
        }
        return lista.distinctBy { it.final8 }
    }
}
