package org.wolfos.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wolfos.launcher.dados.WidgetSalvo
import org.wolfos.launcher.dados.Widgets

/** O que o painel de widgets pede para a tela principal fazer. */
class AcoesWidgets(
    val adicionar: (AppWidgetProviderInfo) -> Unit,
    val remover: (Int) -> Unit,
    val mudarAltura: (Int, Int) -> Unit,
    val voltar: () -> Unit,
)

/**
 * O painel de widgets (deslizar para a esquerda na tela inicial): os widgets um embaixo
 * do outro, cada um com o menu de maior, menor e remover. Deslizar para a direita volta.
 */
@Composable
fun WidgetsTela(salvos: List<WidgetSalvo>, acoes: AcoesWidgets) {
    var escolhendo by remember { mutableStateOf(false) }
    AreaDeGestos(
        aoDeslizar = { dx, dy -> if (direcao(dx, dy) == 0 && dx > 0) acoes.voltar() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            Cabecalho("Widgets", acoes.voltar)
            if (salvos.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
                ) {
                    Icon(Icons.Outlined.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Text(
                        "Ponha aqui o clima, o calendário, as notas, a bateria do fone…",
                        color = TextoFraco, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            for (w in salvos) {
                key(w.id) { WidgetAndroid(w, acoes) }
            }
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { escolhendo = true }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Adicionar widget", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (escolhendo) {
        EscolherWidget(
            aoEscolher = { info -> escolhendo = false; acoes.adicionar(info) },
            aoFechar = { escolhendo = false },
        )
    }
}

/** Um widget: o nome e o menu em cima, e a vista do app embaixo. */
@Composable
fun WidgetAndroid(w: WidgetSalvo, acoes: AcoesWidgets) {
    val contexto = LocalContext.current
    val info = remember(w.id) { Widgets.info(contexto, w.id) }
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Text(
                info?.loadLabel(contexto.packageManager) ?: "Widget indisponível",
                color = TextoFraco, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { menu = true },
                ) { Icon(Icons.Outlined.MoreVert, contentDescription = "Opções", tint = TextoFraco, modifier = Modifier.size(20.dp)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Maior") },
                        leadingIcon = { Icon(Icons.Outlined.UnfoldMore, contentDescription = null) },
                        onClick = { acoes.mudarAltura(w.id, 40) },
                    )
                    DropdownMenuItem(
                        text = { Text("Menor") },
                        leadingIcon = { Icon(Icons.Outlined.UnfoldLess, contentDescription = null) },
                        onClick = { acoes.mudarAltura(w.id, -40) },
                    )
                    DropdownMenuItem(
                        text = { Text("Remover", color = Perigo) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Perigo) },
                        onClick = { menu = false; acoes.remover(w.id) },
                    )
                }
            }
        }
        if (info != null) {
            AndroidView(
                factory = { ctx -> Widgets.anfitriao(ctx).createView(ctx, w.id, info) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(w.altura.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
        } else {
            Text(
                "O app deste widget foi removido.",
                color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(8.dp),
            )
        }
    }
}

/** A lista dos widgets que os apps oferecem, com a previa de cada um. */
@Composable
fun EscolherWidget(aoEscolher: (AppWidgetProviderInfo) -> Unit, aoFechar: () -> Unit) {
    val contexto = LocalContext.current
    val lista by produceState(emptyList<AppWidgetProviderInfo>()) {
        value = withContext(Dispatchers.IO) { Widgets.disponiveis(contexto) }
    }
    Dialog(onDismissRequest = aoFechar) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Superficie)
                .padding(vertical = 18.dp),
        ) {
            Text("Adicionar widget", color = Texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp))
            if (lista.isEmpty()) Text("Lendo os widgets…", color = TextoFraco, modifier = Modifier.padding(22.dp))
            LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
                items(lista, key = { it.provider.flattenToString() }) { info -> OpcaoWidget(info) { aoEscolher(info) } }
            }
        }
    }
}

@Composable
private fun OpcaoWidget(info: AppWidgetProviderInfo, aoTocar: () -> Unit) {
    val contexto = LocalContext.current
    val previa by produceState<ImageBitmap?>(null, info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = info.loadPreviewImage(contexto, 0) ?: info.loadIcon(contexto, 0)
                val w = 220
                val h = if (d.intrinsicWidth > 0) (w * d.intrinsicHeight / d.intrinsicWidth).coerceIn(60, 260) else 120
                d.toBitmap(w, h).asImageBitmap()
            }.getOrNull()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 96.dp, height = 64.dp).clip(RoundedCornerShape(12.dp)).background(Fundo.copy(alpha = 0.5f)),
        ) {
            previa?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp)) }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(info.loadLabel(contexto.packageManager), color = Texto, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(Widgets.nomeDoApp(contexto, info), color = TextoFraco, fontSize = 12.5.sp, maxLines = 1)
        }
    }
}
