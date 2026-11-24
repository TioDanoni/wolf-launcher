package org.wolfos.launcher.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Verbo

/** O que da para fazer com um app (a tela principal liga cada acao). */
class AcoesApp(
    val abrir: (AppInfo) -> Unit,
    val verboDe: (AppInfo) -> Verbo?,
    val mudarVerbo: (AppInfo, Verbo?) -> Unit,
    val informacoes: (AppInfo) -> Unit,
    val desinstalar: (AppInfo) -> Unit,
)

/** O menu de segurar um app: mudar de verbo, informacoes e desinstalar. */
@Composable
fun MenuApp(app: AppInfo, aberto: Boolean, aoFechar: () -> Unit, acoes: AcoesApp) {
    var escolhendo by remember(aberto) { mutableStateOf(false) }
    DropdownMenu(expanded = aberto, onDismissRequest = aoFechar) {
        val item = @Composable { texto: String, marcado: Boolean, acao: () -> Unit ->
            DropdownMenuItem(
                text = { Text(texto, color = if (marcado) MaterialTheme.colorScheme.primary else Texto) },
                onClick = acao,
            )
        }
        val atual = acoes.verboDe(app)
        if (escolhendo) {
            // a lista de verbos no lugar do menu
            for (v in Verbo.entries) item(v.titulo, v == atual) { aoFechar(); acoes.mudarVerbo(app, v) }
            item("Nenhum (só em todos os apps)", atual == null) { aoFechar(); acoes.mudarVerbo(app, null) }
        } else {
            item("Verbo: ${atual?.titulo ?: "nenhum"} ›", false) { escolhendo = true }
            item("Informações do app", false) { aoFechar(); acoes.informacoes(app) }
            item("Desinstalar", false) { aoFechar(); acoes.desinstalar(app) }
        }
    }
}
