package org.wolfos.launcher.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.wolfos.launcher.dados.AppInfo

/** O que da para fazer com um app (a tela principal liga cada acao). */
class AcoesApp(
    val abrir: (AppInfo) -> Unit,
    val ehFavorito: (AppInfo) -> Boolean,
    val alternarFavorito: (AppInfo) -> Unit,
    val mover: (AppInfo, Int) -> Unit,
    val informacoes: (AppInfo) -> Unit,
    val desinstalar: (AppInfo) -> Unit,
)

/** O menu de segurar um app: favoritar, mudar de lugar, informacoes e desinstalar. */
@Composable
fun MenuApp(app: AppInfo, aberto: Boolean, aoFechar: () -> Unit, acoes: AcoesApp, naTelaInicial: Boolean) {
    DropdownMenu(expanded = aberto, onDismissRequest = aoFechar) {
        val item = @Composable { texto: String, acao: () -> Unit ->
            DropdownMenuItem(text = { Text(texto) }, onClick = { aoFechar(); acao() })
        }
        item(if (acoes.ehFavorito(app)) "Tirar da tela inicial" else "Pôr na tela inicial") { acoes.alternarFavorito(app) }
        if (naTelaInicial) {
            item("Subir") { acoes.mover(app, -1) }
            item("Descer") { acoes.mover(app, 1) }
        }
        item("Informações do app") { acoes.informacoes(app) }
        item("Desinstalar") { acoes.desinstalar(app) }
    }
}
