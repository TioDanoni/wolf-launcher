package org.wolfos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.filtrar

/**
 * Todos os apps: a busca no topo, a lista em ordem alfabetica com a letra de cada
 * secao e, na direita, a barra de letras (tocar ou arrastar pula direto).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Gaveta(apps: List<AppInfo>, acoes: AcoesApp, busca: String, aoBuscar: (String) -> Unit) {
    val estado = rememberLazyListState()
    val escopo = rememberCoroutineScope()
    val lista = filtrar(apps, busca)
    val agrupado = busca.isBlank()
    val secoes = if (agrupado) lista.groupBy { it.letra } else emptyMap()
    // indice de cada letra na lista (cabecalho + itens das letras de antes)
    val indices = remember(lista, agrupado) {
        var i = 0
        secoes.mapValues { (_, itens) -> i.also { i += itens.size + 1 } }
    }
    var letraAtual by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        OutlinedTextField(
            value = busca,
            onValueChange = aoBuscar,
            placeholder = { Text("Buscar apps") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            // Enter abre o primeiro resultado
            keyboardActions = KeyboardActions(onGo = { lista.firstOrNull()?.let(acoes.abrir) }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Texto.copy(alpha = 0.12f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
        Row(Modifier.fillMaxSize()) {
            LazyColumn(state = estado, modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (agrupado) {
                    secoes.forEach { (letra, itens) ->
                        stickyHeader(key = "letra-$letra") {
                            Text(
                                letra,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
                            )
                        }
                        itens.forEach { app -> item(key = app.chave) { LinhaApp(app, acoes) } }
                    }
                } else {
                    lista.forEach { app -> item(key = app.chave) { LinhaApp(app, acoes) } }
                    if (lista.isEmpty()) {
                        item {
                            Text("Nenhum app com esse nome.", color = TextoFraco, modifier = Modifier.padding(24.dp))
                        }
                    }
                }
            }
            if (agrupado && secoes.size > 1) {
                BarraDeLetras(
                    letras = secoes.keys.toList(),
                    atual = letraAtual,
                    aoEscolher = { letra ->
                        letraAtual = letra
                        indices[letra]?.let { escopo.launch { estado.scrollToItem(it) } }
                    },
                    aoSoltar = { letraAtual = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaApp(app: AppInfo, acoes: AcoesApp) {
    var menu by remember(app.chave) { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { acoes.abrir(app) }, onLongClick = { menu = true })
                .padding(horizontal = 24.dp, vertical = 9.dp),
        ) {
            IconeApp(app, 40.dp)
            Spacer(Modifier.width(16.dp))
            Text(app.nome, fontSize = 18.sp, color = Texto, maxLines = 1)
        }
        MenuApp(app, menu, { menu = false }, acoes)
    }
}

/** A coluna de letras na direita: tocar ou arrastar o dedo escolhe a letra. */
@Composable
private fun BarraDeLetras(letras: List<String>, atual: String?, aoEscolher: (String) -> Unit, aoSoltar: () -> Unit) {
    var altura by remember { mutableStateOf(1) }
    val letraEm = { y: Float ->
        val i = (y / altura * letras.size).toInt().coerceIn(0, letras.size - 1)
        letras[i]
    }
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(32.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .pointerInput(letras) {
                altura = size.height.coerceAtLeast(1)
                detectTapGestures(onPress = { o ->
                    aoEscolher(letraEm(o.y))
                    tryAwaitRelease()
                    aoSoltar()
                })
            }
            .pointerInput(letras) {
                altura = size.height.coerceAtLeast(1)
                detectVerticalDragGestures(
                    onDragStart = { o -> aoEscolher(letraEm(o.y)) },
                    onDragEnd = aoSoltar,
                    onDragCancel = aoSoltar,
                ) { mudanca, _ -> aoEscolher(letraEm(mudanca.position.y)) }
            },
    ) {
        for (letra in letras) {
            val escolhida = letra == atual
            Text(
                letra,
                fontSize = if (escolhida) 16.sp else 11.sp,
                fontWeight = if (escolhida) FontWeight.Bold else FontWeight.Medium,
                color = if (escolhida) MaterialTheme.colorScheme.primary else TextoFraco,
            )
        }
    }
}
