package org.wolfos.launcher.dados

import android.content.Context
import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/** Uma nota rapida ou uma tarefa (com caixinha de feito). */
@Immutable
data class Nota(val id: Long, val texto: String, val tarefa: Boolean, val feita: Boolean, val criada: Long)

/**
 * As notas e tarefas rapidas do Painel, guardadas so no celular. Da para criar pelo
 * Painel ou pela busca ("nota comprar filamento", "tarefa ligar pro banco").
 */
class Notas(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("notas", Context.MODE_PRIVATE)

    fun ler(): List<Nota> = runCatching {
        val a = JSONArray(prefs.getString("lista", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Nota(o.getLong("id"), o.getString("texto"), o.optBoolean("tarefa", true), o.optBoolean("feita"), o.optLong("criada"))
        }
    }.getOrDefault(emptyList())

    private fun gravar(lista: List<Nota>) {
        val a = JSONArray()
        lista.forEach { n ->
            a.put(JSONObject().put("id", n.id).put("texto", n.texto).put("tarefa", n.tarefa).put("feita", n.feita).put("criada", n.criada))
        }
        prefs.edit().putString("lista", a.toString()).apply()
    }

    fun adicionar(texto: String, tarefa: Boolean = true): List<Nota> {
        val t = texto.trim()
        if (t.isEmpty()) return ler()
        val agora = System.currentTimeMillis()
        val nova = ler() + Nota(agora, t.replaceFirstChar { it.uppercase() }, tarefa, false, agora)
        gravar(nova)
        return nova
    }

    fun alternar(id: Long): List<Nota> = ler().map { if (it.id == id) it.copy(feita = !it.feita) else it }.also(::gravar)

    fun apagar(id: Long): List<Nota> = ler().filter { it.id != id }.also(::gravar)

    /** Tira as tarefas ja feitas. */
    fun limparFeitas(): List<Nota> = ler().filter { !(it.tarefa && it.feita) }.also(::gravar)
}
