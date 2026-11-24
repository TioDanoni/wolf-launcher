package org.wolfos.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Letras
import org.wolfos.launcher.dados.Ponto
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Uma area onde o dedo pode desenhar uma letra. Traco quase reto e um deslizar
 * ([aoDeslizar] com o deslocamento); traco curvo vira letra ([aoLetra] com as mais
 * provaveis e onde o dedo terminou). O rastro aparece na cor de destaque.
 */
@Composable
fun AreaDeDesenho(
    aoDeslizar: (dx: Float, dy: Float) -> Unit,
    aoLetra: (letras: List<Char>, fim: Offset) -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable BoxScope.() -> Unit,
) {
    val traco = remember { mutableStateListOf<Offset>() }
    // o pointerInput vive mais que uma composicao: le sempre as acoes mais novas
    val deslizar by rememberUpdatedState(aoDeslizar)
    val letra by rememberUpdatedState(aoLetra)
    val cor = MaterialTheme.colorScheme.primary
    val minimo = with(LocalDensity.current) { 48.dp.toPx() }
    Box(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { traco.clear(); traco.add(it) },
                onDragEnd = {
                    val pontos = traco.map { Ponto(it.x, it.y) }
                    val fim = traco.lastOrNull() ?: Offset.Zero
                    val inicio = traco.firstOrNull() ?: Offset.Zero
                    traco.clear()
                    when {
                        Letras.comprimento(pontos) < minimo -> Unit
                        Letras.ehReto(pontos) -> deslizar(fim.x - inicio.x, fim.y - inicio.y)
                        else -> Letras.reconhecer(pontos).takeIf { it.isNotEmpty() }?.let { letra(it, fim) }
                    }
                },
                onDragCancel = { traco.clear() },
            ) { mudanca, _ -> traco.add(mudanca.position) }
        },
    ) {
        conteudo()
        // o rastro so aparece quando ja da para ver que e desenho (nao um deslizar reto)
        Canvas(Modifier.fillMaxSize()) {
            if (traco.size < 3) return@Canvas
            val pontos = traco.map { Ponto(it.x, it.y) }
            if (Letras.ehReto(pontos) || Letras.comprimento(pontos) < minimo) return@Canvas
            val caminho = Path().apply {
                moveTo(traco[0].x, traco[0].y)
                for (i in 1 until traco.size) lineTo(traco[i].x, traco[i].y)
            }
            drawPath(caminho, cor.copy(alpha = 0.25f), style = Stroke(22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(caminho, cor, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Deslizar reto: para cima, para baixo, ou nada (lateral fica para depois). */
fun direcao(dx: Float, dy: Float): Int = when {
    abs(dy) <= abs(dx) -> 0
    dy < 0 -> -1
    else -> 1
}

/**
 * O leque: os apps que comecam com as letras desenhadas, em arco em volta de onde o
 * dedo terminou. Desenhar outra letra por cima afina ("S" -> "SP"). Se a letra veio
 * errada, as alternativas ficam embaixo.
 */
@Composable
fun Leque(
    apps: List<AppInfo>,
    prefixo: String,
    alternativas: List<Char>,
    centro: Offset,
    aoAbrir: (AppInfo) -> Unit,
    aoTrocarLetra: (Char) -> Unit,
    aoMaisLetras: (List<Char>) -> Unit,
    aoVerTodos: () -> Unit,
    aoFechar: () -> Unit,
) {
    val cor = MaterialTheme.colorScheme.primary
    val termo = prefixo.lowercase()
    val achados = remember(apps, termo) { apps.filter { it.busca.startsWith(termo) } }
    val mostrar = achados.take(6)

    AreaDeDesenho(
        aoDeslizar = { _, _ -> aoFechar() },
        aoLetra = { letras, _ -> aoMaisLetras(letras) },
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo.copy(alpha = 0.6f))
            .clickable(interactionSource = null, indication = null, onClick = aoFechar),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val d = LocalDensity.current
            val raio = 124.dp
            val (cx, cy) = with(d) {
                val margem = (raio + 44.dp).toPx()
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val x = if (w > 2 * margem) centro.x.coerceIn(margem, w - margem) else w / 2
                val y = centro.y.coerceIn((raio + 110.dp).toPx(), h - 170.dp.toPx())
                x to y
            }
            val raioPx = with(d) { raio.toPx() }
            mostrar.forEachIndexed { i, app ->
                val ang = Math.toRadians(if (mostrar.size == 1) 270.0 else 190.0 + 160.0 * i / (mostrar.size - 1))
                val x = cx + raioPx * cos(ang).toFloat()
                val y = cy + raioPx * sin(ang).toFloat()
                ItemLeque(app, primeiro = i == 0, x, y) { aoAbrir(app) }
            }
            // a letra no centro
            val tamanho = 60.dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset((cx - tamanho.toPx() / 2).toInt(), (cy - tamanho.toPx() / 2).toInt()) }
                    .size(tamanho)
                    .clip(CircleShape)
                    .background(cor),
            ) { Text(prefixo.uppercase(), color = Fundo, fontSize = if (prefixo.length > 2) 20.sp else 30.sp, fontWeight = FontWeight.Bold) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 24.dp),
            ) {
                Text(
                    when {
                        achados.isEmpty() -> "Nenhum app começa com “${prefixo.uppercase()}”"
                        else -> "toque num app · desenhe outra letra para afinar"
                    },
                    color = Texto.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    if (achados.size > mostrar.size) Pilula("ver os ${achados.size}", destaque = true, aoTocar = aoVerTodos)
                    if (alternativas.isNotEmpty()) {
                        Text("era outra?", color = TextoFraco, fontSize = 13.sp)
                        alternativas.forEach { l -> Pilula(l.toString(), destaque = false) { aoTrocarLetra(l) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemLeque(app: AppInfo, primeiro: Boolean, x: Float, y: Float, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    val largura = 88.dp
    val icone = if (primeiro) 54.dp else 46.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { IntOffset((x - largura.toPx() / 2).toInt(), (y - icone.toPx() / 2).toInt()) }
            .width(largura)
            .clip(CircleShape)
            .clickable(onClick = aoTocar),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (primeiro) Modifier.clip(CircleShape).border(3.dp, cor.copy(alpha = 0.5f), CircleShape).padding(4.dp) else Modifier,
        ) { IconeApp(app, icone) }
        Text(
            app.nome,
            color = Texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun Pilula(texto: String, destaque: Boolean, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (destaque) cor.copy(alpha = 0.16f) else Texto.copy(alpha = 0.07f))
            .border(1.dp, if (destaque) cor else Texto.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = aoTocar)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(texto, color = if (destaque) cor else Texto, fontSize = 13.5.sp, fontWeight = FontWeight.Medium) }
}
