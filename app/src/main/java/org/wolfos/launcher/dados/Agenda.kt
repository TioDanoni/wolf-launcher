package org.wolfos.launcher.dados

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat

/** Um compromisso da agenda do Android. */
@Immutable
data class Evento(
    val id: Long,
    val titulo: String,
    val inicio: Long,
    val fim: Long,
    val diaInteiro: Boolean,
    val local: String,
    /** Link de reuniao (Meet, Zoom, Teams), se tiver: vira o botao "Entrar". */
    val link: String?,
)

object Agenda {
    private val LINK = Regex("""https?://(meet\.google\.com|[\w.-]*zoom\.us|teams\.microsoft\.com|teams\.live\.com)/\S+""")

    fun temPermissao(contexto: Context) =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Os compromissos de agora ate [horas] na frente (os que ja comecaram e nao acabaram entram). */
    fun proximos(contexto: Context, horas: Int = 24): List<Evento> {
        if (!temPermissao(contexto)) return emptyList()
        val agora = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, agora - 12 * 3_600_000L)
            ContentUris.appendId(it, agora + horas * 3_600_000L)
        }.build()
        val campos = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
        )
        val lista = mutableListOf<Evento>()
        runCatching {
            contexto.contentResolver.query(
                uri, campos, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val fim = c.getLong(3)
                    if (fim < agora) continue
                    val local = c.getString(5).orEmpty()
                    val descricao = c.getString(6).orEmpty()
                    lista += Evento(
                        id = c.getLong(0),
                        titulo = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(sem título)",
                        inicio = c.getLong(2),
                        fim = fim,
                        diaInteiro = c.getInt(4) == 1,
                        local = local,
                        link = LINK.find(local)?.value ?: LINK.find(descricao)?.value,
                    )
                }
            }
        }
        return lista
    }

    fun abrir(contexto: Context, e: Evento) = Apps.abrirTela(
        contexto,
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.inicio)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.fim),
    )

    fun entrar(contexto: Context, e: Evento) =
        e.link?.let { Apps.abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse(it))) } ?: abrir(contexto, e)
}
