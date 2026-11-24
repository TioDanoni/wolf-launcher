package org.wolfos.launcher.dados

import android.content.Context

/**
 * O que a pessoa mudou no launcher: apps renomeados e escondidos, verbos com outro nome
 * e verbos escondidos da tela inicial. Fica so no celular.
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

    fun verbosEscondidos(): Set<Verbo> =
        prefs.getStringSet("verbos_escondidos", emptySet()).orEmpty().mapNotNull { n -> Verbo.entries.firstOrNull { it.name == n } }.toSet()

    fun esconderVerbo(v: Verbo, esconder: Boolean) {
        val atual = verbosEscondidos().map { it.name }.toSet()
        prefs.edit().putStringSet("verbos_escondidos", if (esconder) atual + v.name else atual - v.name).apply()
    }

    /** O nome de cada verbo (o escolhido ou o padrao). */
    fun titulos(): Map<Verbo, String> = Verbo.entries.associateWith { v -> prefs.getString("titulo|${v.name}", null) ?: v.titulo }

    fun renomearVerbo(v: Verbo, titulo: String?) {
        val e = prefs.edit()
        if (titulo.isNullOrBlank() || titulo.trim() == v.titulo) e.remove("titulo|${v.name}") else e.putString("titulo|${v.name}", titulo.trim())
        e.apply()
    }
}
