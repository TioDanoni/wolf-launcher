package org.wolfos.launcher.dados

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** O ritmo do dia: modo noite, fone conectado e o proximo despertador. */
object Ritmo {
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
