package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.semAcento

/** Lista de apps com busca, para escolher um (usada para adicionar num verbo). */
@Composable
fun EscolherApps(titulo: String, apps: List<AppInfo>, aoEscolher: (AppInfo) -> Unit, aoFechar: () -> Unit) {
    var busca by remember { mutableStateOf("") }
    val lista = remember(apps, busca) {
        val t = semAcento(busca.trim())
        if (t.isEmpty()) apps else apps.filter { it.busca.contains(t) }
    }
    Dialog(onDismissRequest = aoFechar) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Superficie)
                .padding(vertical = 18.dp),
        ) {
            Text(titulo, color = Texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = busca,
                onValueChange = { busca = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Texto, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { campo ->
                    Box {
                        if (busca.isEmpty()) Text("Buscar…", color = TextoFraco, fontSize = 15.sp)
                        campo()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Fundo.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(lista, key = { it.chave }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { aoEscolher(app) }.padding(horizontal = 22.dp, vertical = 9.dp),
                    ) {
                        IconeApp(app, 36.dp)
                        Text(app.nome, color = Texto, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 14.dp))
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                "Pronto",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).padding(end = 22.dp, top = 8.dp).clip(CircleShape).clickable(onClick = aoFechar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
