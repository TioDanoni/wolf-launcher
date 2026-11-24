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
import org.wolfos.launcher.dados.Verbo
import org.wolfos.launcher.dados.semAcento

/** O que da para mudar num verbo. */
class AcoesVerbo(
    val renomear: (Verbo, String?) -> Unit,
    val esconder: (Verbo, Boolean) -> Unit,
    /** Poe o app neste verbo (ou tira, com nulo). */
    val mudarVerbo: (AppInfo, Verbo?) -> Unit,
)

/**
 * Segurar um verbo: o nome, os apps que entram nele (tirar e adicionar), mostrar ou
 * esconder da tela inicial e voltar ao nome padrao.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarVerbo(
    verbo: Verbo,
    titulo: String,
    apps: List<AppInfo>,
    todos: List<AppInfo>,
    escondido: Boolean,
    acoes: AcoesVerbo,
    aoFechar: () -> Unit,
) {
    var renomeando by remember { mutableStateOf(false) }
    var adicionando by remember { mutableStateOf(false) }

    Folha(aoFechar) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) { IconeVerbo(verbo, Modifier.size(28.dp)) }
            Column(Modifier.padding(start = 16.dp)) {
                Text(titulo, color = Texto, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(descricaoDoVerbo(verbo), color = TextoFraco, fontSize = 13.sp)
            }
        }

        SecaoMenu("Apps neste verbo")
        if (apps.isEmpty()) {
            Text("Nenhum app por enquanto.", color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            for (app in apps) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Texto.copy(alpha = 0.06f))
                        .border(1.dp, Texto.copy(alpha = 0.1f), CircleShape)
                        .clickable { acoes.mudarVerbo(app, null) }
                        .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    IconeApp(app, 24.dp)
                    Text(app.nome, color = Texto, fontSize = 13.5.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp))
                    Icon(Icons.Outlined.Close, contentDescription = "Tirar", tint = TextoFraco, modifier = Modifier.size(16.dp))
                }
            }
            Pilula("+ Adicionar", destaque = true) { adicionando = true }
        }
        Text(
            "Tocar num app tira ele deste verbo (ele continua em todos os apps).",
            color = TextoFraco, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        Divisoria(Modifier.padding(top = 6.dp))
        LinhaMenu(Icons.Outlined.DriveFileRenameOutline, "Renomear", "o nome que aparece na tela inicial") { renomeando = !renomeando }
        if (renomeando) {
            CampoNome(titulo, verbo.titulo.takeIf { it != titulo }) { novo -> acoes.renomear(verbo, novo); renomeando = false }
        }
        LinhaMenu(
            Icons.Outlined.Visibility,
            "Mostrar na tela inicial",
            if (escondido) "escondido: os apps continuam na busca e na lista" else "aparece com os outros verbos",
            fim = { ChaveWolf(!escondido) { acoes.esconder(verbo, !it) } },
        ) { acoes.esconder(verbo, !escondido) }
        if (titulo != verbo.titulo) {
            LinhaMenu(Icons.Outlined.Restore, "Voltar ao nome “${verbo.titulo}”") { acoes.renomear(verbo, null) }
        }
    }

    if (adicionando) {
        EscolherApps(
            titulo = "Adicionar em $titulo",
            apps = todos.filter { it !in apps },
            aoEscolher = { acoes.mudarVerbo(it, verbo) },
            aoFechar = { adicionando = false },
        )
    }
}

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
