package org.wolfos.launcher.dados

import android.content.Context

/** Os ajustes do launcher (os que nao sao de um app especifico). */
class Preferencias(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Baixar e instalar a versao nova sozinho (senao, so avisa). */
    var instalarSozinho: Boolean
        get() = prefs.getBoolean("instalar_sozinho", true)
        set(v) = prefs.edit().putBoolean("instalar_sozinho", v).apply()

    var ultimaVerificacao: Long
        get() = prefs.getLong("ultima_verificacao", 0L)
        set(v) = prefs.edit().putLong("ultima_verificacao", v).apply()
}
