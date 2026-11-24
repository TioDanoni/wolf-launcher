package org.wolfos.launcher.dados

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** A bateria agora: porcentagem e se esta carregando (le o aviso fixo do Android, sem permissao). */
object Bateria {
    fun estado(contexto: Context): Pair<Int, Boolean>? {
        val i = runCatching { contexto.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull() ?: return null
        val nivel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (nivel < 0 || escala <= 0) return null
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val carregando = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return (nivel * 100 / escala) to carregando
    }
}
