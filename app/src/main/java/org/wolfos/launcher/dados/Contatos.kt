package org.wolfos.launcher.dados

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat

/** Uma pessoa da agenda, com o numero principal. */
@Immutable
data class Contato(val id: Long, val nome: String, val numero: String, val favorito: Boolean) {
    val busca: String = semAcento(nome)
}

object Contatos {
    fun temPermissao(contexto: Context) =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /**
     * Todos os contatos com telefone (um por pessoa, favoritos primeiro). A busca e feita
     * na memoria, sem acento: "mae" acha "Mãe", o que o LIKE do banco nao faz.
     */
    fun carregar(contexto: Context): List<Contato> {
        if (!temPermissao(contexto)) return emptyList()
        val campos = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
        )
        val lista = mutableListOf<Contato>()
        val vistos = HashSet<Long>()
        runCatching {
            contexto.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, campos, null, null,
                // o numero principal de cada pessoa vem primeiro
                "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val nome = c.getString(1) ?: continue
                    val numero = c.getString(2) ?: continue
                    if (vistos.add(id)) lista += Contato(id, nome, numero, c.getInt(3) == 1)
                }
            }
        }
        return lista.sortedWith(compareByDescending<Contato> { it.favorito }.thenBy { it.busca })
    }

    /** Os contatos que batem com o termo (comecando com ele primeiro). */
    fun filtrar(contatos: List<Contato>, termo: String): List<Contato> {
        val t = semAcento(termo.trim())
        if (t.isEmpty()) return emptyList()
        return contatos
            .filter { c -> c.busca.startsWith(t) || c.busca.split(' ').any { it.startsWith(t) } }
            .sortedBy { if (it.busca.startsWith(t)) 0 else 1 }
    }

    /** Abre o discador com o numero (a pessoa so toca em ligar). */
    fun ligar(contexto: Context, c: Contato) =
        Apps.abrirTela(contexto, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(c.numero))))

    fun mensagem(contexto: Context, c: Contato) =
        Apps.abrirTela(contexto, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(c.numero))))
}
