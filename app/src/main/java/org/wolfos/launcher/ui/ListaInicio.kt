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
import androidx.compose.foundation.layout.height
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
    /** Muda a cada toque no Home: a lista volta para os favoritos. */
    reinicio: Int = 0,
    modifier: Modifier = Modifier,
) {
    var letra by remember { mutableStateOf<String?>(null) }
    var noTrilho by remember { mutableStateOf(false) }
    // a contagem para voltar: so anda com o dedo fora do alfabeto
    val volta = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(letra, noTrilho) {
        if (letra != null && !noTrilho) {
            volta.snapTo(1f)
            volta.animateTo(0f, tween(VOLTA_MS, easing = LinearEasing))
            letra = null
        } else {
            volta.snapTo(1f)
        }
    }
    LaunchedEffect(reinicio) { letra = null }
    // o voltar do Android tambem volta para os favoritos
    androidx.activity.compose.BackHandler(enabled = letra != null) { letra = null }
    // tocar num app da letra abre e ja deixa a lista pronta nos favoritos
    val acoesLista = remember(acoes) {
        AcoesApp(
            abrir = { a -> letra = null; acoes.abrir(a) },
            informacoes = acoes.informacoes,
            desinstalar = acoes.desinstalar,
            renomear = acoes.renomear,
            esconder = acoes.esconder,
            abrirAtalho = acoes.abrirAtalho,
            ehFavorito = acoes.ehFavorito,
            alternarFavorito = acoes.alternarFavorito,
            avisosDe = acoes.avisosDe,
            acoesAviso = acoes.acoesAviso,
        )
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                        Text(l, fontSize = 30.sp, fontWeight = FontWeight.Bold, style = TextStyle(brush = degrade(), shadow = SombraTexto))
                        Text("  ${itens.size} app(s)", color = TextoFraco, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(CircleShape).clickable { letra = null },
                        ) { Icon(Icons.Outlined.Close, contentDescription = "Voltar aos favoritos", tint = Texto, modifier = Modifier.size(20.dp)) }
                    }
                    // a linha fina que encolhe: quanto falta para voltar aos favoritos
                    Box(
                        Modifier
                            .padding(start = 8.dp, end = 12.dp, top = 2.dp, bottom = 6.dp)
                            .fillMaxWidth(volta.value.coerceIn(0f, 1f))
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(degradeH()),
                    )
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
                                acoes = acoesLista,
                            )
                        }
                    }
                }
            }
        }
        AlfabetoLateral(
            letras = letras,
            aoEscolher = { l -> noTrilho = true; letra = l },
            aoSoltar = { noTrilho = false },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/** Quanto tempo a lista de uma letra fica depois de soltar o dedo do alfabeto. */
private const val VOLTA_MS = 3500

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
                IconeApp(app, 32.dp)
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
