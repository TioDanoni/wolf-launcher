package org.wolfos.launcher.dados

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** O momento do dia, que decide a ordem dos verbos. */
enum class Momento { MANHA, TARDE, NOITE }

/**
 * O ritmo do dia: de manha sobe o Ir e o Ouvir, de tarde o Trabalhar, a noite o
 * Conversar e o Ver. Com o fone conectado, o Ouvir vai para o topo. Com o tempo, o que
 * a pessoa mais abre naquela hora pesa mais que a ordem padrao.
 */
object Ritmo {
    private val BASE = mapOf(
        Momento.MANHA to listOf(Verbo.IR, Verbo.OUVIR, Verbo.CONVERSAR, Verbo.TRABALHAR, Verbo.VER, Verbo.CRIAR, Verbo.JOGAR),
        Momento.TARDE to listOf(Verbo.TRABALHAR, Verbo.CONVERSAR, Verbo.IR, Verbo.OUVIR, Verbo.CRIAR, Verbo.VER, Verbo.JOGAR),
        Momento.NOITE to listOf(Verbo.CONVERSAR, Verbo.VER, Verbo.OUVIR, Verbo.JOGAR, Verbo.CRIAR, Verbo.IR, Verbo.TRABALHAR),
    )

    /** No modo noite so ficam os verbos calmos (o resto continua na gaveta). */
    val CALMOS = listOf(Verbo.OUVIR, Verbo.VER, Verbo.CONVERSAR)

    fun momento(hora: Int): Momento = when (hora) {
        in 5..11 -> Momento.MANHA
        in 12..17 -> Momento.TARDE
        else -> Momento.NOITE
    }

    fun ordenar(
        verbos: Map<Verbo, List<AppInfo>>,
        hora: Int,
        pontos: Map<Verbo, Int>,
        fone: Boolean,
        calmo: Boolean,
        ligado: Boolean,
    ): Map<Verbo, List<AppInfo>> {
        if (calmo) return CALMOS.mapNotNull { v -> verbos[v]?.let { v to it } }.toMap()
        if (!ligado) return verbos
        val base = BASE.getValue(momento(hora))
        val total = pontos.values.sum()
        val maior = (pontos.values.maxOrNull() ?: 0).coerceAtLeast(1)
        fun nota(v: Verbo): Double {
            var n = (base.size - base.indexOf(v)).toDouble()
            // depois de umas aberturas, o costume da pessoa naquela hora pesa
            if (total >= 8) n += 4.0 * (pontos[v] ?: 0) / maior
            if (fone && v == Verbo.OUVIR) n += 100.0
            return n
        }
        return verbos.entries.sortedByDescending { nota(it.key) }.associate { it.key to it.value }
    }

    /** O modo noite esta valendo agora? (janela em minutos do dia; pode virar a meia-noite) */
    fun ehNoite(minutoDoDia: Int, inicio: Int, fim: Int): Boolean =
        if (inicio <= fim) minutoDoDia in inicio until fim else minutoDoDia >= inicio || minutoDoDia < fim

    fun foneConectado(contexto: Context): Boolean {
        val audio = contexto.getSystemService(AudioManager::class.java) ?: return false
        val tipos = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        return audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in tipos }
    }

    /** O proximo despertador marcado no relogio do Android (nao precisa de permissao). */
    fun proximoAlarme(contexto: Context): Pair<Long, PendingIntent?>? {
        val alarme = contexto.getSystemService(AlarmManager::class.java)?.nextAlarmClock ?: return null
        return alarme.triggerTime to alarme.showIntent
    }
}
