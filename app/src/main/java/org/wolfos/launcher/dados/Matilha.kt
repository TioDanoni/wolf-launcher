package org.wolfos.launcher.dados

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import org.wolfos.launcher.avisos.Aviso

/**
 * A Matilha: as pessoas que ficam no polegar. A pessoa escolhe quem entra; enquanto
 * nao escolher ninguem, entram os favoritos (estrela) da agenda.
 */
class Matilha(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("matilha", Context.MODE_PRIVATE)

    fun ler(): List<Long> =
        prefs.getString("ids", "").orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }

    fun gravar(ids: List<Long>) {
        prefs.edit().putString("ids", ids.distinct().joinToString(",")).apply()
    }

    companion object {
        const val MAXIMO = 8

        /** Quem aparece: os escolhidos (na ordem) ou, sem escolha, os favoritos da agenda. */
        fun pessoas(contatos: List<Contato>, escolhidos: List<Long>): List<Contato> {
            if (escolhidos.isNotEmpty()) {
                val porId = contatos.associateBy { it.id }
                return escolhidos.mapNotNull { porId[it] }
            }
            return contatos.filter { it.favorito }.take(5)
        }

        /** As notificacoes que parecem ser desta pessoa (pelo nome no titulo). */
        fun esperando(contato: Contato, avisos: List<Aviso>): List<Aviso> = avisos.filter { a ->
            val t = semAcento(a.titulo)
            t.isNotEmpty() && (t == contato.busca || t.startsWith(contato.busca + " ") ||
                (t.length >= 3 && contato.busca.startsWith(t)))
        }

        fun primeiroNome(contato: Contato): String = contato.nome.trim().substringBefore(' ')

        fun verNaAgenda(contexto: Context, contato: Contato) = Apps.abrirTela(
            contexto,
            Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contato.id.toString())),
        )

        /** Conversa no WhatsApp, se tiver instalado (numero sem +, assume Brasil). */
        fun whatsapp(contexto: Context, contato: Contato): Boolean {
            val pacote = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { p ->
                runCatching { contexto.packageManager.getPackageInfo(p, 0) }.isSuccess
            } ?: return false
            val digitos = contato.numero.filter(Char::isDigit)
            val internacional = if (contato.numero.trim().startsWith("+")) digitos else "55" + digitos.trimStart('0')
            return Apps.abrirTela(
                contexto,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$internacional")).setPackage(pacote),
            )
        }

        fun temWhatsapp(contexto: Context): Boolean = listOf("com.whatsapp", "com.whatsapp.w4b").any { p ->
            runCatching { contexto.packageManager.getPackageInfo(p, 0) }.isSuccess
        }
    }
}
