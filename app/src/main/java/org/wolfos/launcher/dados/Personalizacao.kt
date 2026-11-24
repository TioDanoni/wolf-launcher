package org.wolfos.launcher.dados

import android.content.Context

/**
 * O que a pessoa mudou no launcher: apps renomeados e escondidos. Fica so no celular.
 */
class Personalizacao(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("personalizacao", Context.MODE_PRIVATE)

    /** chave do app -> nome escolhido. */
    fun nomes(): Map<String, String> = prefs.all
        .filterKeys { it.startsWith("nome|") }
        .mapNotNull { (k, v) -> (v as? String)?.let { k.removePrefix("nome|") to it } }
        .toMap()

    fun renomear(chave: String, nome: String?) {
        val e = prefs.edit()
        if (nome.isNullOrBlank()) e.remove("nome|$chave") else e.putString("nome|$chave", nome.trim())
        e.apply()
    }

    fun escondidos(): Set<String> = prefs.getStringSet("escondidos", emptySet()).orEmpty().toSet()

    fun esconder(chave: String, esconder: Boolean) {
        val atual = escondidos()
        prefs.edit().putStringSet("escondidos", if (esconder) atual + chave else atual - chave).apply()
    }
}
