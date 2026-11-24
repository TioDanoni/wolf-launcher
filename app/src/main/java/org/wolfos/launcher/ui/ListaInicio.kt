package org.wolfos.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.dados.AppInfo

/**
 * A lista de apps da tela inicial: os favoritos (e, completando, os sugeridos para a
 * hora), cada um com o icone, o nome, a ultima mensagem e o numero de notificacoes.
 * Na direita, o alfabeto: arrastar troca a lista para os apps daquela letra; o X (ou
 * alguns segundos parado) volta para os favoritos.
 */
@Composable
fun ListaInicio(
    favoritos: List<AppInfo>,
    sugeridos: List<AppInfo>,
    todos: List<AppInfo>,
    avisos: Map<String, Int>,
    /** A ultima mensagem de cada pacote ("Ana: chego em 10 min"). */
    ultimas: Map<String, String>,
    /** O pacote que esta tocando musica agora (ganha as barrinhas). */
    tocando: String?,
    acoes: AcoesApp,
    modifier: Modifier = Modifier,
) {
    var letra by remember { mutableStateOf<String?>(null) }
    var mexeu by remember { mutableStateOf(0) }
    // parado numa letra por um tempo: volta sozinho para os favoritos
    LaunchedEffect(letra, mexeu) {
        if (letra != null) {
            delay(9000)
            letra = null
        }
    }
    val letras = remember(todos) { todos.map { it.letra }.distinct() }

    Row(modifier) {
        AnimatedContent(
            targetState = letra,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 } + fadeOut(tween(160)))
            },
            label = "lista",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) { l ->
            val itens = if (l == null) favoritos.map { it to false } + sugeridos.map { it to true }
            else todos.filter { it.letra == l }.map { it to false }
            Column(Modifier.fillMaxSize()) {
                if (l != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)) {
                        Text(l, fontSize = 30.sp, fontWeight = FontWeight.Bold, style = TextStyle(brush = degrade(), shadow = SombraTexto))
                        Text("  ${itens.size} app(s)", color = TextoFraco, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)).clickable { letra = null },
                        ) { Icon(Icons.Outlined.Close, contentDescription = "Voltar aos favoritos", tint = Texto, modifier = Modifier.size(18.dp)) }
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(itens, key = { _, (a, s) -> a.chave + s }) { i, (app, sugerido) ->
                        Box(Modifier.entrada(i.coerceAtMost(10), atraso = 35L)) {
                            LinhaInicio(
                                app = app,
                                avisos = avisos[app.pacote] ?: 0,
                                ultima = ultimas[app.pacote],
                                sugerido = sugerido,
                                tocando = app.pacote == tocando,
                                acoes = acoes,
                            )
                        }
                    }
                }
            }
        }
        AlfabetoLateral(
            letras = letras,
            aoEscolher = { l -> letra = l; mexeu++ },
            aoSoltar = { mexeu++ },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/** Uma linha da lista: icone, nome, a ultima mensagem embaixo e o numero de notificacoes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaInicio(app: AppInfo, avisos: Int, ultima: String?, sugerido: Boolean, tocando: Boolean, acoes: AcoesApp) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember(app.chave) { mutableStateOf(false) }
    val toque = remember { MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apertado) 0.96f else 1f, label = "escala")
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .clip(RoundedCornerShape(16.dp))
                .background(if (apertado) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                .combinedClickable(interactionSource = toque, indication = null, onClick = { acoes.abrir(app) }, onLongClick = { menu = true })
                .padding(horizontal = 6.dp, vertical = 7.dp),
        ) {
            Box {
                IconeApp(app, 34.dp)
                if (avisos > 0) Selo(avisos, Modifier.align(Alignment.TopEnd).padding(start = 0.dp).size(17.dp).graphicsLayer { translationX = 8.dp.toPx(); translationY = (-6).dp.toPx() })
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.nome,
                        color = Texto,
                        fontSize = 16.5.sp,
                        fontWeight = if (avisos > 0 || tocando) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = SombraTexto),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (tocando) Barrinhas(cor, Modifier.padding(start = 10.dp))
                    if (sugerido) Box(Modifier.padding(start = 8.dp).size(5.dp).clip(CircleShape).background(cor.copy(alpha = 0.7f)))
                }
                if (!ultima.isNullOrBlank()) {
                    Text(
                        ultima,
                        color = cor,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = SombraTexto),
                    )
                }
            }
        }
        MenuApp(app, menu, { menu = false }, acoes)
    }
}

/** Tres barrinhas que dancam enquanto a musica toca. */
@Composable
private fun Barrinhas(cor: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "barrinhas")
    val alturas = listOf(520, 380, 640).map { d ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(d, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    }
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val w = size.width / 5
        alturas.forEachIndexed { i, a ->
            val h = size.height * a.value
            drawRoundRect(cor, topLeft = Offset(i * w * 2, size.height - h), size = Size(w, h), cornerRadius = CornerRadius(w / 2))
        }
    }
}

/** O resumo das notificacoes, ao lado da Matilha: o sino, quantas e de quem e a ultima. */
@Composable
fun ResumoAvisos(total: Int, ultima: String?, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    // o sino balanca um pouco quando ha notificacao
    val t = rememberInfiniteTransition(label = "sino")
    val giro by t.animateFloat(-10f, 10f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "giro")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = aoTocar).padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            if (total > 0) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = if (total > 0) cor else TextoFraco,
            modifier = Modifier.size(22.dp).graphicsLayer { if (total > 0) rotationZ = giro },
        )
        Column(Modifier.padding(start = 10.dp)) {
            Text(
                when (total) {
                    0 -> "Nenhuma"
                    1 -> "1 notificação"
                    else -> "$total notificações"
                },
                color = Texto, fontSize = 13.sp, fontWeight = if (total > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, style = TextStyle(shadow = SombraTexto),
            )
            Text(
                if (total == 0) "notificação" else ultima.orEmpty(),
                color = if (total > 0) cor else TextoFraco, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = SombraTexto),
            )
        }
    }
}
