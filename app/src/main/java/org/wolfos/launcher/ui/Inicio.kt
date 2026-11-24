package org.wolfos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Verbo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")
private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val SEMANA = DateTimeFormatter.ofPattern("EEEE", PT)
private val DATA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT)

/** Relogio que muda na virada de cada minuto (sem acordar o celular a toa). */
@Composable
fun agora(): LocalDateTime {
    val atual by produceState(LocalDateTime.now()) {
        while (true) {
            val agora = LocalDateTime.now()
            value = agora
            delay((60 - agora.second) * 1000L)
        }
    }
    return atual
}

/** O que a tela inicial pede para a tela principal fazer. */
class AcoesInicio(
    val abrirVerbo: (Verbo) -> Unit,
    val abrirComando: () -> Unit,
    val abrirGaveta: () -> Unit,
    val abrirNotificacoes: () -> Unit,
    val abrirAjustes: () -> Unit,
    val papelDeParede: () -> Unit,
    val atualizar: () -> Unit,
)

/**
 * A tela inicial: relogio, a faixa do agora, os verbos (o que voce quer fazer) e a
 * barra de comando na altura do polegar. Para cima abre todos os apps; para baixo,
 * as notificacoes; segurar num espaco vazio abre o menu do launcher.
 */
@Composable
fun Inicio(verbos: Map<Verbo, List<AppInfo>>, atualizacao: EstadoAtualizacao, acoes: AcoesInicio) {
    var arrasto by remember { mutableStateOf(Offset.Zero) }
    var menuEm by remember { mutableStateOf<Offset?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { arrasto = Offset.Zero },
                    onDragEnd = {
                        val (dx, dy) = arrasto
                        if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                            if (dy < -120f) acoes.abrirGaveta() else if (dy > 120f) acoes.abrirNotificacoes()
                        }
                        arrasto = Offset.Zero
                    },
                    onDragCancel = { arrasto = Offset.Zero },
                ) { _, d -> arrasto += d }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { menuEm = it })
            },
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
            Relogio(Modifier.padding(start = 10.dp, top = 28.dp))
            textoAtualizacao(atualizacao)?.let { (simbolo, texto, botao) ->
                Spacer(Modifier.height(16.dp))
                Faixa(simbolo, texto, botao, acoes.atualizar)
            }
            // os verbos ficam embaixo, perto do polegar; se nao couberem, rolam
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.align(Alignment.BottomStart).verticalScroll(rememberScrollState()),
                ) {
                    for (v in verbos.keys) {
                        Text(
                            v.titulo,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            color = Texto,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { acoes.abrirVerbo(v) }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                        )
                    }
                    if (verbos.isEmpty()) {
                        Text("Lendo seus apps…", color = TextoFraco, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            BarraComandoFechada(acoes.abrirComando)
            Text(
                "︿ todos os apps  ·  segure para os ajustes",
                color = TextoFraco.copy(alpha = 0.7f),
                fontSize = 11.5.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp),
            )
        }

        // o menu de segurar num espaco vazio, aberto onde o dedo estava
        menuEm?.let { o ->
            Box(Modifier.offset { IntOffset(o.x.toInt(), o.y.toInt()) }) {
                DropdownMenu(expanded = true, onDismissRequest = { menuEm = null }) {
                    DropdownMenuItem(text = { Text("Ajustes do WolfLauncher") }, onClick = { menuEm = null; acoes.abrirAjustes() })
                    DropdownMenuItem(text = { Text("Papel de parede") }, onClick = { menuEm = null; acoes.papelDeParede() })
                    DropdownMenuItem(text = { Text("Todos os apps") }, onClick = { menuEm = null; acoes.abrirGaveta() })
                }
            }
        }
    }
}

@Composable
private fun Relogio(modifier: Modifier = Modifier) {
    val momento = agora()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(momento.format(HORA), fontSize = 56.sp, fontWeight = FontWeight.ExtraLight, color = Texto)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                momento.format(SEMANA).replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(momento.format(DATA), color = TextoFraco, fontSize = 13.sp)
        }
    }
}
