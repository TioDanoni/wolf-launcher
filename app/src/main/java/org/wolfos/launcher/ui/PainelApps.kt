package org.wolfos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Icones

/**
 * O painel de apps da tela inicial: "Agora" (o que faz sentido neste momento, escolhido
 * pelo launcher) e "Favoritos" (os que a pessoa fixa). Cada icone tem uma aura da cor
 * dele por tras, e o numero de notificacoes em cima.
 */
@Composable
fun PainelApps(
    agora: List<AppInfo>,
    favoritos: List<AppInfo>,
    /** Os favoritos foram escolhidos pela pessoa (senao, sao os mais usados). */
    escolhidos: Boolean,
    avisos: Map<String, Int>,
    acoes: AcoesApp,
    aoAdicionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.18f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))),
                forma,
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        var indice = 0
        if (agora.isNotEmpty()) {
            Cabeca("Agora", null)
            Fileira(agora, avisos, acoes, indice)
            indice += agora.size
            Spacer(Modifier.height(10.dp))
        }
        Cabeca("Favoritos", if (escolhidos) null else "segure um app para fixar", aoAdicionar)
        favoritos.chunked(4).forEach { linha ->
            Fileira(linha, avisos, acoes, indice)
            indice += linha.size
        }
    }
}

@Composable
private fun Cabeca(titulo: String, dica: String?, aoAdicionar: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 4.dp)) {
        Text(titulo, color = Texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(shadow = SombraTexto))
        Box(Modifier.padding(start = 8.dp).size(width = 18.dp, height = 2.dp).clip(CircleShape).background(degradeH()))
        if (dica != null) {
            Text(dica, color = TextoFraco, fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.padding(start = 10.dp))
        }
        Box(Modifier.weight(1f))
        if (aoAdicionar != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = aoAdicionar),
            ) { Icon(Icons.Outlined.Add, contentDescription = "Adicionar favorito", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp)) }
        }
    }
}

/** Quatro lugares por fileira (sobra espaco quando ha menos de quatro, sem esticar). */
@Composable
private fun Fileira(apps: List<AppInfo>, avisos: Map<String, Int>, acoes: AcoesApp, inicio: Int) {
    Row(Modifier.fillMaxWidth()) {
        for (i in 0 until 4) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                apps.getOrNull(i)?.let { app ->
                    Box(Modifier.entrada(inicio + i, atraso = 40L)) { AppComAura(app, avisos[app.pacote] ?: 0, acoes) }
                }
            }
        }
    }
}

/** O icone colorido com a aura da cor dele, o nome embaixo e o selo de notificacoes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppComAura(app: AppInfo, avisos: Int, acoes: AcoesApp) {
    val contexto = LocalContext.current
    val cor by produceState(Icones.corDaMemoria(app), app.chave) {
        if (value == null) value = Icones.cor(contexto, app)
    }
    val aura = cor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    var menu by remember(app.chave) { mutableStateOf(false) }
    val toque = remember { MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apertado) 0.9f else 1f, label = "escala")
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(78.dp)
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(interactionSource = toque, indication = null, onClick = { acoes.abrir(app) }, onLongClick = { menu = true })
                .padding(vertical = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    // a aura: um brilho redondo da cor do icone, por tras dele
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(aura.copy(alpha = 0.55f), aura.copy(alpha = 0.18f), Color.Transparent),
                                center = center,
                                radius = size.minDimension * 0.62f,
                            ),
                            radius = size.minDimension * 0.62f,
                        )
                    },
            ) {
                IconeApp(app, 50.dp)
                if (avisos > 0) Selo(avisos, Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
            }
            Text(
                app.nome,
                color = Texto,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = SombraTexto),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        MenuApp(app, menu, { menu = false }, acoes)
    }
}
