package org.wolfos.launcher.ui

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
    val segunda = MaterialTheme.colorScheme.secondary
    // o tempo anda uma vez por segundo, so enquanto toca
    val posicao by produceState(musica.posicaoAgora(), musica) {
        while (true) {
            value = musica.posicaoAgora()
            if (!musica.tocando) break
            delay(1000)
        }
    }
    val respira = rememberInfiniteTransition(label = "capa")
    val escalaCapa by respira.animateFloat(1f, if (musica.tocando) 1.04f else 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "e")
    val parte by animateFloatAsState(
        if (musica.duracao > 0) (posicao.toFloat() / musica.duracao).coerceIn(0f, 1f) else 0f,
        tween(950, easing = LinearEasing),
        label = "parte",
    )
    // sem cartao por tras: tudo flutua sobre o wallpaper
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer { scaleX = escalaCapa; scaleY = escalaCapa }
                .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = cor, spotColor = cor)
                .clip(RoundedCornerShape(16.dp))
                .background(cor.copy(alpha = 0.18f))
                .clickable(onClick = acoes.abrir),
        ) {
            musica.capa?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(62.dp)) }
                ?: Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = cor)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp).clickable(onClick = acoes.abrir)) {
            Text(
                musica.titulo, color = Texto, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = SombraTexto),
            )
            Text(
                listOf(musica.artista, if (musica.duracao > 0) "${tempo(posicao)} / ${tempo(musica.duracao)}" else "")
                    .filter { it.isNotBlank() }.joinToString("  ·  "),
                color = Texto.copy(alpha = 0.62f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = SombraTexto),
            )
        }
        Botao(Icons.Rounded.SkipPrevious, "Anterior", acoes.anterior)
        // tocar/pausar: so o contorno, com o anel do tempo em volta (tocar no anel nao pula)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .clickable(onClick = acoes.tocarOuPausar)
                .drawBehind {
                    val traco = 2.dp.toPx()
                    val r = size.minDimension / 2 - traco
                    drawCircle(Texto.copy(alpha = 0.22f), radius = r, style = Stroke(traco))
                    drawArc(
                        Brush.sweepGradient(listOf(cor, segunda, cor)),
                        startAngle = -90f,
                        sweepAngle = 360f * parte,
                        useCenter = false,
                        topLeft = Offset(traco, traco),
                        size = Size(size.width - 2 * traco, size.height - 2 * traco),
                        style = Stroke(traco, cap = StrokeCap.Round),
                    )
                },
        ) {
            Icon(
                if (musica.tocando) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (musica.tocando) "Pausar" else "Tocar",
                tint = Texto,
                modifier = Modifier.size(26.dp),
            )
        }
        Botao(Icons.Rounded.SkipNext, "Próxima", acoes.proxima)
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
