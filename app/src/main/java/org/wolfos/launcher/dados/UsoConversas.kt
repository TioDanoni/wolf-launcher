package org.wolfos.launcher.dados

import android.content.Context

/** Quanto e quando uma conversa foi usada. */
data class UsoConversa(val abertas: Int, val ultimaAbertura: Long, val mensagens: Int, val ultimaMensagem: Long)

/**
 * O uso das conversas, guardado so no celular: quantas vezes a pessoa abriu cada uma
 * pela Matilha e quantas mensagens chegaram dela. Serve para a Matilha mostrar quem
 * importa (mais usadas e mais recentes) em vez de so a ordem do WhatsApp.
 */
class UsoConversas(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("uso_conversas", Context.MODE_PRIVATE)

    fun registrarAbertura(chave: String) {
        prefs.edit()
            .putInt("a|$chave", prefs.getInt("a|$chave", 0) + 1)
            .putLong("ta|$chave", System.currentTimeMillis())
            .apply()
    }

    fun registrarMensagem(chave: String) {
        prefs.edit()
            .putInt("m|$chave", prefs.getInt("m|$chave", 0) + 1)
            .putLong("tm|$chave", System.currentTimeMillis())
            .apply()
    }

    fun ler(): Map<String, UsoConversa> {
        val tudo = prefs.all
        val chaves = tudo.keys.mapNotNull { k -> k.substringAfter('|', "").takeIf { it.isNotEmpty() } }.toSet()
        return chaves.associateWith { c ->
            UsoConversa(
                abertas = tudo["a|$c"] as? Int ?: 0,
                ultimaAbertura = tudo["ta|$c"] as? Long ?: 0L,
                mensagens = tudo["m|$c"] as? Int ?: 0,
                ultimaMensagem = tudo["tm|$c"] as? Long ?: 0L,
            )
        }
    }
}
