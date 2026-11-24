package org.wolfos.launcher.dados

import android.content.Context

/** Os ajustes do launcher (os que nao sao de um app especifico). */
class Preferencias(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Baixar e instalar a versao nova sozinho (senao, so avisa). */
    var instalarSozinho: Boolean
        get() = prefs.getBoolean("instalar_sozinho", true)
        set(v) = prefs.edit().putBoolean("instalar_sozinho", v).apply()

    /** A fileira da Matilha na tela inicial. */
    var mostrarMatilha: Boolean
        get() = prefs.getBoolean("mostrar_matilha", true)
        set(v) = prefs.edit().putBoolean("mostrar_matilha", v).apply()

    /** Ja pediu uma vez, sozinho, para ser a tela inicial (depois so pela faixa). */
    var jaPediuPadrao: Boolean
        get() = prefs.getBoolean("ja_pediu_padrao", false)
        set(v) = prefs.edit().putBoolean("ja_pediu_padrao", v).apply()

    var ultimaVerificacao: Long
        get() = prefs.getLong("ultima_verificacao", 0L)
        set(v) = prefs.edit().putLong("ultima_verificacao", v).apply()
}
