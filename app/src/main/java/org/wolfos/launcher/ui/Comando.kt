package org.wolfos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.filtrar
import org.wolfos.launcher.dados.semAcento

/** Um resultado da barra de comando. */
private sealed interface Resultado {
    data class App(val app: AppInfo) : Resultado
    data object Ajustes : Resultado
    data class Web(val termo: String) : Resultado
}

/**
 * A barra de comando aberta. A barra fica colada no teclado e os resultados sobem a
 * partir dela: o melhor fica mais perto do polegar, e o Enter abre ele.
 */
@Composable
fun Comando(
    apps: List<AppInfo>,
    texto: String,
    aoMudar: (String) -> Unit,
    acoes: AcoesApp,
    aoAjustes: () -> Unit,
    aoWeb: (String) -> Unit,
    aoFechar: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    val termo = texto.trim()
    val resultados = remember(apps, termo) {
        if (termo.isEmpty()) emptyList<Resultado>()
        else buildList<Resultado> {
            filtrar(apps, termo).take(8).forEach { add(Resultado.App(it)) }
            val t = semAcento(termo)
            if ("ajustes do wolflauncher".contains(t) || "wolflauncher".startsWith(t)) add(Resultado.Ajustes)
            add(Resultado.Web(termo))
        }
    }
    val executar: (Resultado) -> Unit = { r ->
        when (r) {
            is Resultado.App -> acoes.abrir(r.app)
            Resultado.Ajustes -> aoAjustes()
            is Resultado.Web -> aoWeb(r.termo)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            // tocar fora dos resultados fecha
            .clickable(interactionSource = null, indication = null, onClick = aoFechar)
            .safeDrawingPadding()
            .padding(horizontal = 14.dp),
    ) {
        // reverseLayout: o primeiro resultado fica embaixo, colado na barra
        LazyColumn(
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            resultados.forEachIndexed { i, r ->
                item(key = chave(r)) { LinhaResultado(r, primeiro = i == 0, acoes) { executar(r) } }
            }
            if (termo.isEmpty()) {
                item {
                    Text(
                        "Escreva o nome de um app.\nEm breve: pessoas, contas e ajustes do Android.",
                        color = TextoFraco,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        Barra(texto, aoMudar, foco, aoFechar) { resultados.firstOrNull()?.let(executar) }
    }
}

private fun chave(r: Resultado): String = when (r) {
    is Resultado.App -> r.app.chave
    Resultado.Ajustes -> "ajustes"
    is Resultado.Web -> "web"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaResultado(r: Resultado, primeiro: Boolean, acoes: AcoesApp, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(18.dp)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(forma)
                .then(if (primeiro) Modifier.background(cor.copy(alpha = 0.15f)).border(1.dp, cor.copy(alpha = 0.4f), forma) else Modifier)
                .combinedClickable(onClick = aoTocar, onLongClick = { if (r is Resultado.App) menu = true })
                .padding(horizontal = 14.dp)
                .height(56.dp),
        ) {
            when (r) {
                is Resultado.App -> IconeApp(r.app, 34.dp)
                Resultado.Ajustes -> Simbolo("⚙")
                is Resultado.Web -> Simbolo("⌕")
            }
            Text(
                when (r) {
                    is Resultado.App -> r.app.nome
                    Resultado.Ajustes -> "Ajustes do WolfLauncher"
                    is Resultado.Web -> "Buscar “${r.termo}” na web"
                },
                color = Texto,
                fontSize = 16.sp,
                fontWeight = if (primeiro) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
            )
            if (primeiro) Text("↵", color = cor, fontWeight = FontWeight.Bold)
        }
        if (r is Resultado.App) MenuApp(r.app, menu, { menu = false }, acoes)
    }
}

@Composable
private fun Simbolo(s: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(34.dp).clip(CircleShape).background(Superficie),
    ) { Text(s, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) }
}

@Composable
private fun Barra(texto: String, aoMudar: (String) -> Unit, foco: FocusRequester, aoFechar: () -> Unit, aoEnviar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Superficie)
            .border(1.dp, cor, CircleShape)
            // tocar na barra (fora do texto) nao fecha: so devolve o foco
            .clickable(interactionSource = null, indication = null) { foco.requestFocus() }
            .padding(start = 12.dp, end = 4.dp)
            .height(54.dp),
    ) {
        MarcaUivo()
        BasicTextField(
            value = texto,
            onValueChange = aoMudar,
            singleLine = true,
            textStyle = TextStyle(color = Texto, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(cor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { aoEnviar() }),
            decorationBox = { campo ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (texto.isEmpty()) Text("Buscar ou mandar…", color = TextoFraco, fontSize = 15.sp)
                    campo()
                }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp).focusRequester(foco),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(46.dp).clip(CircleShape).clickable {
                if (texto.isEmpty()) aoFechar() else aoMudar("")
            },
        ) { Text("✕", color = TextoFraco, fontSize = 15.sp) }
    }
}
