package org.wolfos.launcher.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Verbo

/** O que da para fazer com uma notificacao mostrada pelo launcher. */
class AcoesAviso(
    val abrir: (Aviso) -> Unit,
    val dispensar: (Aviso) -> Unit,
    val permitir: () -> Unit,
)

/**
 * Tocou num verbo: em cima, quem esta esperando (as notificacoes dos apps do verbo);
 * embaixo, os apps em grade, com o numero de notificacoes. Segurar um app muda o verbo.
 */
@Composable
fun VerboTela(
    verbo: Verbo,
    apps: List<AppInfo>,
    avisos: List<Aviso>,
    avisosPermitidos: Boolean,
    acoes: AcoesApp,
    acoesAviso: AcoesAviso,
    aoFechar: () -> Unit,
) {
    val pacotes = remember(apps) { apps.map { it.pacote }.toSet() }
    val doVerbo = remember(avisos, pacotes) { avisos.filter { it.pacote in pacotes } }
    val contagem = remember(doVerbo) { doVerbo.groupingBy { it.pacote }.eachCount() }
    val nomes = remember(apps) { apps.associate { it.pacote to it.nome } }
    val linha = GridItemSpan(3)

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Cabecalho(verbo.titulo, aoFechar)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (doVerbo.isNotEmpty()) {
                item(span = { linha }) { Rotulo("Quem está esperando", Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp)) }
                items(doVerbo.take(8), key = { "aviso:" + it.chave }, span = { linha }) { a ->
                    CartaoAviso(a, nomes[a.pacote], acoesAviso)
                }
            } else if (!avisosPermitidos && verbo in setOf(Verbo.CONVERSAR, Verbo.TRABALHAR)) {
                item(span = { linha }) {
                    Faixa("☺", "Ver aqui quem está esperando resposta", "Permitir", acoesAviso.permitir, Modifier.padding(4.dp))
                }
            }
            item(span = { linha }) {
                Rotulo("Apps para ${verbo.titulo.lowercase()}", Modifier.padding(start = 14.dp, top = 16.dp, bottom = 4.dp))
            }
            items(apps, key = { it.chave }) { app -> AppNaGrade(app, contagem[app.pacote] ?: 0, acoes) }
        }
    }
}

@Composable
private fun CartaoAviso(aviso: Aviso, app: String?, acoes: AcoesAviso) {
    val forma = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(forma)
            .background(Superficie.copy(alpha = 0.85f))
            .border(1.dp, Texto.copy(alpha = 0.07f), forma)
            .clickable { acoes.abrir(aviso) }
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(50.dp).border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(4.dp),
        ) { Avatar(aviso.titulo.ifEmpty { app.orEmpty() }, 42.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    aviso.titulo.ifEmpty { app.orEmpty() },
                    color = Texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "  " + listOfNotNull(app, haQuanto(aviso.quando)).joinToString(" · "),
                    color = TextoFraco,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Text(aviso.texto, color = Texto.copy(alpha = 0.7f), fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { acoes.dispensar(aviso) },
        ) { Text("✕", color = TextoFraco, fontSize = 14.sp) }
    }
}

private fun haQuanto(quando: Long): String {
    val passou = System.currentTimeMillis() - quando
    return if (passou < DateUtils.MINUTE_IN_MILLIS) "agora"
    else DateUtils.getRelativeTimeSpanString(quando, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppNaGrade(app: AppInfo, avisos: Int, acoes: AcoesApp) {
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
            Box {
                IconeApp(app, 52.dp)
                if (avisos > 0) Selo(avisos, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp))
            }
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

/** O numero de notificacoes em cima do icone. */
@Composable
fun Selo(n: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    ) { Text(if (n > 9) "9+" else n.toString(), color = Fundo, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}
