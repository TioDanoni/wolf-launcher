package org.wolfos.launcher.dados

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import androidx.compose.runtime.Immutable

/** As partes do Painel (deslizar para a esquerda), na ordem padrao. */
enum class PartePainel(val titulo: String) {
    CLIMA_BATERIA("Clima e bateria"),
    CONTROLES("Controles rápidos"),
    TIMER("Timer"),
    MUSICA("Música"),
    NOTAS("Notas e tarefas"),
    AGENDA("Próximos compromissos"),
    ARMAZENAMENTO("Armazenamento"),
    WIDGETS("Widgets do Android"),
}

/** A ordem e o que esta escondido no Painel. */
class ArrumacaoPainel(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("painel", Context.MODE_PRIVATE)

    fun ordem(): List<PartePainel> {
        val salvas = prefs.getString("ordem", null)?.split(',')?.mapNotNull { n -> PartePainel.entries.firstOrNull { it.name == n } }.orEmpty()
        // partes novas (de versoes futuras) entram no fim
        return salvas + PartePainel.entries.filter { it !in salvas }
    }

    fun escondidas(): Set<PartePainel> =
        prefs.getStringSet("escondidas", emptySet()).orEmpty().mapNotNull { n -> PartePainel.entries.firstOrNull { it.name == n } }.toSet()

    fun mover(p: PartePainel, passo: Int) {
        val l = ordem().toMutableList()
        val i = l.indexOf(p)
        val j = i + passo
        if (i < 0 || j !in l.indices) return
        l[i] = l[j].also { l[j] = l[i] }
        prefs.edit().putString("ordem", l.joinToString(",") { it.name }).apply()
    }

    fun alternar(p: PartePainel) {
        val e = escondidas()
        prefs.edit().putStringSet("escondidas", (if (p in e) e - p else e + p).map { it.name }.toSet()).apply()
    }
}

/** Espaco do celular: livre e total, em bytes. */
object Armazenamento {
    fun agora(): Pair<Long, Long> = runCatching {
        val s = StatFs(Environment.getDataDirectory().path)
        s.availableBytes to s.totalBytes
    }.getOrDefault(0L to 0L)

    fun texto(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 10) "%.0f GB".format(gb) else "%.1f GB".format(gb)
    }
}

/** Um aparelho Bluetooth conectado (fone, relogio...) e a bateria dele, quando ele informa. */
@Immutable
data class Aparelho(val nome: String, val bateria: Int?)

object Aparelhos {
    /**
     * Os aparelhos Bluetooth conectados agora, com a bateria que eles mandam (o Android
     * guarda, mas so mostra por um metodo escondido). Precisa da permissao de Bluetooth.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun conectados(contexto: Context): List<Aparelho> = runCatching {
        val adaptador = contexto.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        adaptador.bondedDevices.orEmpty().mapNotNull { d ->
            val conectado = runCatching { d.javaClass.getMethod("isConnected").invoke(d) as Boolean }.getOrDefault(false)
            if (!conectado) return@mapNotNull null
            val bateria = runCatching { d.javaClass.getMethod("getBatteryLevel").invoke(d) as Int }.getOrNull()?.takeIf { it in 0..100 }
            Aparelho(d.name ?: "Aparelho", bateria)
        }
    }.getOrDefault(emptyList())
}

/**
 * O timer do Painel: conta regressiva (e Pomodoro) pelo relogio do Android, sem abrir
 * ele, e o cronometro feito no proprio launcher. O fim fica guardado para o Painel
 * mostrar quanto falta.
 */
class TimerWolf(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("timer", Context.MODE_PRIVATE)

    /** Quando a contagem regressiva acaba (0 = nenhuma) e o nome dela. */
    val fim: Long get() = prefs.getLong("fim", 0L).takeIf { it > System.currentTimeMillis() } ?: 0L
    val duracao: Long get() = prefs.getLong("duracao", 0L)
    val rotulo: String get() = prefs.getString("rotulo", "Timer") ?: "Timer"

    fun comecar(contexto: Context, minutos: Int, rotulo: String): Boolean = comecarSegundos(contexto, minutos * 60, rotulo)

    fun comecarSegundos(contexto: Context, segundos: Int, rotulo: String): Boolean {
        val ok = Apps.abrirTela(
            contexto,
            Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, segundos)
                .putExtra(AlarmClock.EXTRA_MESSAGE, rotulo)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
        )
        if (ok) {
            val d = segundos * 1000L
            prefs.edit().putLong("fim", System.currentTimeMillis() + d).putLong("duracao", d).putString("rotulo", rotulo).apply()
        }
        return ok
    }

    /** Esquece a contagem no Painel e abre o relogio para parar la. */
    fun parar(contexto: Context) {
        prefs.edit().putLong("fim", 0L).apply()
        Apps.abrirTela(contexto, Intent(AlarmClock.ACTION_SHOW_TIMERS))
    }

    // cronometro: quando comecou (0 = parado) e o que ja tinha acumulado
    val cronoInicio: Long get() = prefs.getLong("crono_inicio", 0L)
    val cronoAcumulado: Long get() = prefs.getLong("crono_acumulado", 0L)

    fun cronoAlternar() {
        val agora = System.currentTimeMillis()
        if (cronoInicio > 0) {
            prefs.edit().putLong("crono_acumulado", cronoAcumulado + (agora - cronoInicio)).putLong("crono_inicio", 0L).apply()
        } else {
            prefs.edit().putLong("crono_inicio", agora).apply()
        }
    }

    fun cronoZerar() {
        prefs.edit().putLong("crono_inicio", 0L).putLong("crono_acumulado", 0L).apply()
    }
}
