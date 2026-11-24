package org.wolfos.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.avisos.Musica

/** O que o player faz. */
class AcoesPlayer(
    val tocarOuPausar: () -> Unit,
    val anterior: () -> Unit,
    val proxima: () -> Unit,
    val irPara: (Long) -> Unit,
    /** Abre o app que esta tocando. */
    val abrir: () -> Unit,
)

/**
 * O player da tela inicial: aparece quando algo toca (ou acabou de pausar). Capa,
 * titulo, artista, a barra do tempo (tocar nela pula para aquele ponto) e os controles.
 * Tocar na capa ou no nome abre o app que esta tocando.
 */
@Composable
fun Player(musica: Musica, acoes: AcoesPlayer, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    val forma = RoundedCornerShape(22.dp)
    // a barra anda uma vez por segundo, so enquanto toca
    val posicao by produceState(musica.posicaoAgora(), musica) {
        while (true) {
            value = musica.posicaoAgora()
            if (!musica.tocando) break
            delay(1000)
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Superficie.copy(alpha = 0.82f))
            .border(1.dp, Texto.copy(alpha = 0.08f), forma)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(cor.copy(alpha = 0.15f)).clickable(onClick = acoes.abrir),
            ) {
                musica.capa?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp)) }
                    ?: Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = cor)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp).clickable(onClick = acoes.abrir)) {
                Text(musica.titulo, color = Texto, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (musica.artista.isNotBlank()) {
                    Text(musica.artista, color = TextoFraco, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Botao(Icons.Rounded.SkipPrevious, "Anterior", acoes.anterior)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(46.dp).clip(CircleShape).background(cor).clickable(onClick = acoes.tocarOuPausar),
            ) {
                Icon(
                    if (musica.tocando) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (musica.tocando) "Pausar" else "Tocar",
                    tint = Fundo,
                    modifier = Modifier.size(28.dp),
                )
            }
            Botao(Icons.Rounded.SkipNext, "Próxima", acoes.proxima)
        }
        if (musica.duracao > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text(tempo(posicao), color = TextoFraco, fontSize = 11.sp)
                // a barra: tocar pula para aquele ponto
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(18.dp)
                        .pointerInput(musica.duracao) {
                            detectTapGestures { o -> acoes.irPara((o.x / size.width * musica.duracao).toLong()) }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Texto.copy(alpha = 0.15f)))
                    Box(
                        Modifier
                            .fillMaxWidth((posicao.toFloat() / musica.duracao).coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(cor),
                    )
                }
                Text(tempo(musica.duracao), color = TextoFraco, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Botao(icone: androidx.compose.ui.graphics.vector.ImageVector, descricao: String, aoTocar: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = aoTocar),
    ) { Icon(icone, contentDescription = descricao, tint = Texto, modifier = Modifier.size(26.dp)) }
}

/** 3:07, 1:02:45 */
private fun tempo(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
