package org.wolfos.launcher.avisos

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A musica (ou podcast) de agora, com os controles. */
@Immutable
data class Musica(
    val titulo: String,
    val artista: String,
    val pacote: String,
    val tocando: Boolean,
    val capa: ImageBitmap?,
    /** Duracao em ms (0 quando o app nao informa, como em radio ao vivo). */
    val duracao: Long = 0,
    /** Posicao em ms no momento [atualizadoEm] (relogio SystemClock.elapsedRealtime). */
    val posicao: Long = 0,
    val atualizadoEm: Long = 0,
    val velocidade: Float = 1f,
) {
    /** A posicao agora, andando sozinha enquanto toca. */
    fun posicaoAgora(): Long {
        if (!tocando || atualizadoEm == 0L) return posicao
        val andou = ((android.os.SystemClock.elapsedRealtime() - atualizadoEm) * velocidade).toLong()
        return (posicao + andou).let { if (duracao > 0) it.coerceAtMost(duracao) else it }
    }
}

/**
 * O que esta tocando, lido das sessoes de midia do Android. Usa a mesma permissao das
 * notificacoes (o Android so libera as sessoes para quem tem o acesso as notificacoes).
 */
object Tocando {
    private val _musica = MutableStateFlow<Musica?>(null)
    val musica: StateFlow<Musica?> = _musica.asStateFlow()

    private var controle: MediaController? = null

    /** O que o launcher esta vendo (aparece nos ajustes, para entender quando o player nao aparece). */
    private val _detalhe = MutableStateFlow("ainda não procurou")
    val detalhe: StateFlow<String> = _detalhe.asStateFlow()
    private var gerente: MediaSessionManager? = null
    private val principal = Handler(Looper.getMainLooper())

    private val aoMudar = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publicar()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publicar()
        override fun onSessionDestroyed() = escolher(gerente?.let { g -> sessoes(g) }.orEmpty())
    }

    private val aoMudarSessoes = MediaSessionManager.OnActiveSessionsChangedListener { lista ->
        escolher(lista.orEmpty())
    }

    /** Comeca a acompanhar (chamado de novo e seguro: so liga uma vez). */
    fun iniciar(contexto: Context) {
        if (gerente != null) {
            escolher(gerente?.let { sessoes(it) }.orEmpty())
            return
        }
        val g = contexto.getSystemService(MediaSessionManager::class.java) ?: return
        val componente = ComponentName(contexto, Ouvinte::class.java)
        componenteAtual = componente
        runCatching {
            g.addOnActiveSessionsChangedListener(aoMudarSessoes, componente, principal)
            gerente = g
            escolher(g.getActiveSessions(componente))
        }.onFailure { _detalhe.value = "o Android não liberou as sessões (${it.javaClass.simpleName}); usando a notificação do player" }
    }

    /**
     * O segundo caminho: a notificacao de um player traz a sessao dele dentro. Serve
     * quando o Android nao entrega a sessao pela lista (acontece com alguns apps).
     */
    fun daNotificacao(contexto: Context, token: MediaSession.Token) {
        runCatching {
            val c = MediaController(contexto, token)
            val atualTocando = controle?.playbackState?.state == PlaybackState.STATE_PLAYING
            val novaTocando = c.playbackState?.state == PlaybackState.STATE_PLAYING
            // so troca se a nova esta tocando, ou se nao ha nenhuma tocando agora
            if (c.sessionToken != controle?.sessionToken && (novaTocando || !atualTocando)) trocar(c)
            else publicar()
        }
    }

    private var componenteAtual: ComponentName? = null

    private fun sessoes(g: MediaSessionManager): List<MediaController> =
        runCatching { g.getActiveSessions(componenteAtual) }.getOrNull().orEmpty()

    /** Fica com a sessao que esta tocando (ou a primeira, se nenhuma toca). */
    private fun escolher(lista: List<MediaController>) {
        _detalhe.value = if (lista.isEmpty()) "nenhum app de música aberto" else "${lista.size} sessão(ões): " + lista.joinToString { it.packageName }
        val nova = lista.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: lista.firstOrNull()
        // lista vazia mas com sessao vinda da notificacao: fica com ela
        if (nova == null && controle != null) {
            publicar()
            return
        }
        trocar(nova)
    }

    private fun trocar(nova: MediaController?) {
        if (nova?.sessionToken != controle?.sessionToken) {
            runCatching { controle?.unregisterCallback(aoMudar) }
            controle = nova
            nova?.registerCallback(aoMudar, principal)
        }
        publicar()
    }

    private fun publicar() {
        val c = controle
        val m = c?.metadata
        val titulo = (m?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() })
        if (c == null || titulo == null) {
            if (c != null) _detalhe.value = "${c.packageName} sem nome de música (ainda)"
            _musica.value = null
            return
        }
        _detalhe.value = "${if (c.playbackState?.state == PlaybackState.STATE_PLAYING) "tocando" else "pausado"}: $titulo (${c.packageName})"
        val capa: Bitmap? = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val atual = _musica.value
        val estado = c.playbackState
        _musica.value = Musica(
            titulo = titulo,
            artista = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            pacote = c.packageName,
            tocando = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            // a capa so e convertida de novo quando a musica muda
            capa = if (atual?.titulo == titulo && atual.pacote == c.packageName && atual.capa != null) atual.capa
            else capa?.let { runCatching { Bitmap.createScaledBitmap(it, 160, 160, true).asImageBitmap() }.getOrNull() },
            duracao = m.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
            posicao = estado?.position?.coerceAtLeast(0) ?: 0,
            atualizadoEm = estado?.lastPositionUpdateTime ?: 0,
            velocidade = estado?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    fun tocarOuPausar() {
        val c = controle ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun proxima() {
        controle?.transportControls?.skipToNext()
    }

    fun anterior() {
        controle?.transportControls?.skipToPrevious()
    }

    /** Pula para um ponto da musica (em ms). */
    fun irPara(ms: Long) {
        controle?.transportControls?.seekTo(ms)
    }
}
