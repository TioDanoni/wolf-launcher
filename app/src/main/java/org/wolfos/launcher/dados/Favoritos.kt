package org.wolfos.launcher.dados

import android.content.Context

/** Os favoritos da tela inicial, na ordem que a pessoa escolheu (chaves pacote/atividade). */
class Favoritos(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("favoritos", Context.MODE_PRIVATE)

    fun ler(): List<String> =
        prefs.getString(CHAVE, "").orEmpty().split('\n').filter { it.isNotBlank() }

    fun gravar(lista: List<String>) {
        prefs.edit().putString(CHAVE, lista.joinToString("\n")).apply()
    }

    private companion object {
        const val CHAVE = "lista"
    }
}
