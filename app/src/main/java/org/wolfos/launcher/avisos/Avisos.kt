package org.wolfos.launcher.avisos

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uma notificacao que vale mostrar (mensagem, chamada perdida, e-mail...). */
@Immutable
data class Aviso(
    val chave: String,
    val pacote: String,
    /** Quem mandou ou o assunto ("Ana", "Grupo Matilha"). */
    val titulo: String,
    val texto: String,
    val quando: Long,
    val abrir: PendingIntent?,
    /** O atalho de conversa da notificacao (Android 10+): liga a notificacao a conversa certa. */
    val atalho: String? = null,
    /** A acao "Responder" do proprio app (com campo de texto), se tiver. */
    val responder: Notification.Action? = null,
    /** A acao "Marcar como lida", se tiver. */
    val marcarLida: Notification.Action? = null,
)

/** O clima que o app do tempo (Google, Samsung...) deixa na cortina: "Piracicaba: 23°". */
@Immutable
data class Clima(val cidade: String, val graus: Int, val descricao: String)

/**
 * As notificacoes que estao na cortina, para o launcher mostrar junto dos verbos e da
 * Matilha. Quem preenche e o [Ouvinte], que o Android liga quando a pessoa permite.
 */
object Avisos {
    private val _lista = MutableStateFlow<List<Aviso>>(emptyList())

    /** Da mais nova para a mais velha. */
    val lista: StateFlow<List<Aviso>> = _lista.asStateFlow()

    internal var ouvinte: Ouvinte? = null

    private val _clima = MutableStateFlow<Clima?>(null)
    val clima: StateFlow<Clima?> = _clima.asStateFlow()

    private val CLIMA = Regex("""^(.+?):\s*(-?\d+)\s*°""")

    fun permitido(contexto: Context): Boolean =
        contexto.packageName in NotificationManagerCompat.getEnabledListenerPackages(contexto)

    /** Abre a tela do Android para permitir o acesso as notificacoes. */
    fun pedirPermissao(contexto: Context) {
        val componente = ComponentName(contexto, Ouvinte::class.java).flattenToString()
        val direto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componente)
        } else {
            null
        }
        val geral = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        for (intent in listOfNotNull(direto, geral)) {
            if (runCatching { contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    /** Abre o que a notificacao abriria (a conversa, a chamada...). */
    fun abrir(contexto: Context, aviso: Aviso): Boolean {
        val pi = aviso.abrir ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // o launcher esta na tela: empresta a permissao de abrir para o app da notificacao
                val opcoes = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pi.send(contexto, 0, null, null, null, null, opcoes.toBundle())
            } else {
                pi.send()
            }
        }.isSuccess
    }

    /** A ultima mensagem de uma notificacao de conversa: (quem mandou, texto). */
    private fun ultimaMensagem(extras: android.os.Bundle): Pair<String?, String>? {
        val msgs = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }.getOrNull() ?: return null
        val ultima = msgs.lastOrNull() as? android.os.Bundle ?: return null
        val texto = ultima.getCharSequence("text")?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val quem = ultima.getCharSequence("sender")?.toString()
            ?: runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    (ultima.getParcelable<android.app.Person>("sender_person"))?.name?.toString()
                } else null
            }.getOrNull()
        return quem?.trim() to texto
    }

    /** A acao com campo de texto livre (a de responder). */
    private fun acaoDeResponder(n: Notification): Notification.Action? {
        val comTexto = n.actions.orEmpty().filter { a -> a.remoteInputs?.any { it.allowFreeFormInput } == true }
        return comTexto.firstOrNull { a ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && a.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
        } ?: comTexto.firstOrNull()
    }

    /** Responde pela propria notificacao, como o "Responder" da cortina. */
    fun responder(contexto: Context, aviso: Aviso, texto: String): Boolean {
        val acao = aviso.responder ?: return false
        val entradas = acao.remoteInputs ?: return false
        return runCatching {
            val resultado = Intent()
            val valores = android.os.Bundle().apply { entradas.forEach { putCharSequence(it.resultKey, texto) } }
            android.app.RemoteInput.addResultsToIntent(entradas, resultado, valores)
            acao.actionIntent.send(contexto, 0, resultado)
        }.isSuccess
    }

    fun marcarComoLida(aviso: Aviso): Boolean = runCatching { aviso.marcarLida?.actionIntent?.send() }.isSuccess

    /** Tira a notificacao da cortina (como deslizar ela para o lado). */
    fun dispensar(aviso: Aviso) {
        runCatching { ouvinte?.cancelNotification(aviso.chave) }
    }

    internal fun publicar(notificacoes: Array<StatusBarNotification>?, meuPacote: String) {
        val todas = notificacoes.orEmpty()
        // a notificacao do tempo vira o clima ao lado da data (e sai das previas)
        var clima: Clima? = null
        val semClima = todas.filter { sbn ->
            val c = climaDe(sbn)
            if (c != null && clima == null) clima = c
            c == null
        }
        _clima.value = clima
        _lista.value = semClima
            .filter { valeMostrar(it, meuPacote) }
            .mapNotNull(::converter)
            .sortedByDescending { it.quando }
    }

    private fun climaDe(sbn: StatusBarNotification): Clima? {
        val e = sbn.notification.extras
        val titulo = e.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return null
        val m = CLIMA.find(titulo) ?: return null
        val texto = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        return Clima(m.groupValues[1].trim(), m.groupValues[2].toIntOrNull() ?: return null, texto.substringBefore('·').trim())
    }

    internal fun limpar() {
        _lista.value = emptyList()
    }

    private fun valeMostrar(sbn: StatusBarNotification, meuPacote: String): Boolean {
        val n = sbn.notification
        if (sbn.packageName == meuPacote || !sbn.isClearable) return false
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        // musica tocando, downloads, "procurando atualizacoes"... nao sao conversa
        return n.category !in setOf(
            Notification.CATEGORY_TRANSPORT, Notification.CATEGORY_PROGRESS, Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_SYSTEM, Notification.CATEGORY_STATUS,
        )
    }

    private fun converter(sbn: StatusBarNotification): Aviso? {
        val extras = sbn.notification.extras
        // conversa (WhatsApp, Telegram...): a ultima mensagem de verdade, com quem mandou
        val ultima = ultimaMensagem(extras)
        val grupo = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val conversa = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val titulo = (conversa ?: ultima?.first?.takeIf { !grupo }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())?.trim()
        val texto = when {
            ultima != null && grupo && ultima.first != null -> "${ultima.first}: ${ultima.second}"
            ultima != null -> ultima.second
            else -> (extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.trim().orEmpty()
        }
        if (titulo.isNullOrEmpty() && texto.isEmpty()) return null
        return Aviso(
            chave = sbn.key,
            pacote = sbn.packageName,
            titulo = titulo.orEmpty(),
            texto = texto,
            quando = sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime,
            abrir = sbn.notification.contentIntent,
            atalho = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sbn.notification.shortcutId else null,
            responder = acaoDeResponder(sbn.notification),
            marcarLida = sbn.notification.actions?.firstOrNull { a ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && a.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ
            },
        )
    }
}

/** O servico que o Android liga quando a pessoa permite o acesso as notificacoes. */
class Ouvinte : NotificationListenerService() {
    override fun onListenerConnected() {
        Avisos.ouvinte = this
        atualizar()
        Tocando.iniciar(applicationContext)
        runCatching { activeNotifications }.getOrNull()?.forEach(::olharPlayer)
    }

    override fun onListenerDisconnected() {
        Avisos.ouvinte = null
        Avisos.limpar()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        atualizar()
        sbn?.let(::olharPlayer)
    }

    /** A notificacao de um player traz a sessao de midia: o player da tela inicial usa. */
    private fun olharPlayer(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        } ?: return
        Tocando.daNotificacao(applicationContext, token)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = atualizar()

    private fun atualizar() {
        Avisos.publicar(runCatching { activeNotifications }.getOrNull(), packageName)
    }
}
