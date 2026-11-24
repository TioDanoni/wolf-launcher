package org.wolfos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Verbo

/**
 * Tocou num verbo: os apps dele em grade. (As pessoas que estao esperando, em cima,
 * chegam junto com a Matilha.) Segurar um app muda o verbo dele.
 */
@Composable
fun VerboTela(verbo: Verbo, apps: List<AppInfo>, acoes: AcoesApp, aoFechar: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Cabecalho(verbo.titulo, aoFechar)
        Rotulo("Apps para ${verbo.titulo.lowercase()}", Modifier.padding(start = 26.dp, top = 12.dp, bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.chave }) { app -> AppNaGrade(app, acoes) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppNaGrade(app: AppInfo, acoes: AcoesApp) {
    var menu by remember(app.chave) { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(onClick = { acoes.abrir(app) }, onLongClick = { menu = true })
                .padding(vertical = 14.dp, horizontal = 4.dp),
        ) {
            IconeApp(app, 52.dp)
            Text(
                app.nome,
                color = Texto.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        MenuApp(app, menu, { menu = false }, acoes)
    }
}
