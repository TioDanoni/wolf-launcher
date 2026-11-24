package org.wolfos.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
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

/** O que da para fazer com uma notificacao mostrada pelo launcher. */
class AcoesAviso(
    val abrir: (Aviso) -> Unit,
    val dispensar: (Aviso) -> Unit,
    val permitir: () -> Unit,
    /** Devolve se deu para mandar. */
    val responder: (Aviso, String) -> Boolean = { _, _ -> false },
    val marcarLida: (Aviso) -> Unit = {},
)

/** Uma mensagem esperando: abrir, dispensar, responder ali mesmo e marcar como lida. */
@Composable
fun CartaoAviso(aviso: Aviso, app: String?, acoes: AcoesAviso) {
    val cor = MaterialTheme.colorScheme.primary
    val forma = RoundedCornerShape(22.dp)
    var respondendo by remember(aviso.chave) { mutableStateOf(false) }
    var texto by remember(aviso.chave) { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(forma)
            .background(Superficie.copy(alpha = 0.85f))
            .border(1.dp, Texto.copy(alpha = 0.07f), forma),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { acoes.abrir(aviso) }
                .padding(start = 14.dp, top = 12.dp, bottom = if (aviso.responder != null || aviso.marcarLida != null) 4.dp else 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(50.dp).border(2.5.dp, cor, CircleShape).padding(4.dp),
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
                Text(aviso.texto, color = Texto.copy(alpha = 0.7f), fontSize = 13.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { acoes.dispensar(aviso) },
            ) { Text("✕", color = TextoFraco, fontSize = 14.sp) }
        }
        // responder e marcar como lida, sem abrir o app
        if (aviso.responder != null || aviso.marcarLida != null) {
            if (respondendo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 76.dp, end = 10.dp, bottom = 12.dp),
                ) {
                    BasicTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        textStyle = TextStyle(color = Texto, fontSize = 15.sp),
                        cursorBrush = SolidColor(cor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (texto.isNotBlank() && acoes.responder(aviso, texto)) { texto = ""; respondendo = false }
                        }),
                        decorationBox = { campo ->
                            Box {
                                if (texto.isEmpty()) Text("Responder…", color = TextoFraco, fontSize = 15.sp)
                                campo()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Fundo.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cor)
                            .clickable {
                                if (texto.isNotBlank() && acoes.responder(aviso, texto)) { texto = ""; respondendo = false }
                            },
                    ) { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar", tint = Fundo, modifier = Modifier.size(20.dp)) }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 76.dp, bottom = 12.dp),
                ) {
                    if (aviso.responder != null) Pilula("Responder", destaque = true) { respondendo = true }
                    if (aviso.marcarLida != null) Pilula("Marcar como lida", destaque = false) { acoes.marcarLida(aviso) }
                }
            }
        }
    }
}

private fun haQuanto(quando: Long): String {
    val passou = System.currentTimeMillis() - quando
    return if (passou < DateUtils.MINUTE_IN_MILLIS) "agora"
    else DateUtils.getRelativeTimeSpanString(quando, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
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
