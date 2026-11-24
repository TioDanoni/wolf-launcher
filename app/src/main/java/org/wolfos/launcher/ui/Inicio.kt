package org.wolfos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.dados.AppInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DATA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

/** Relogio que muda na virada de cada minuto (sem acordar o celular a toa). */
@Composable
private fun agora(): LocalDateTime {
    val atual by produceState(LocalDateTime.now()) {
        while (true) {
            val agora = LocalDateTime.now()
            value = agora
            delay((60 - agora.second) * 1000L)
        }
    }
    return atual
}

/**
 * A tela inicial: relogio, data e os favoritos em texto grande. Deslizar para cima
 * abre a gaveta com todos os apps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Inicio(favoritos: List<AppInfo>, acoes: AcoesApp, aoAbrirGaveta: () -> Unit) {
    var arrasto by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (arrasto < -120f) aoAbrirGaveta()
                        arrasto = 0f
                    },
                    onDragCancel = { arrasto = 0f },
                ) { _, dy -> arrasto += dy }
            }
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        val momento = agora()
        Text(momento.format(HORA), fontSize = 64.sp, fontWeight = FontWeight.Light, color = Texto)
        Text(
            momento.format(DATA).replaceFirstChar { it.uppercase() },
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        if (favoritos.isEmpty()) {
            Text(
                "Segure um app na lista para pôr ele aqui.",
                color = TextoFraco,
                fontSize = 15.sp,
            )
        }
        for (app in favoritos) {
            var menu by remember(app.chave) { mutableStateOf(false) }
            Box {
                Text(
                    app.nome,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Texto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { acoes.abrir(app) }, onLongClick = { menu = true })
                        .padding(vertical = 10.dp),
                )
                MenuApp(app, menu, { menu = false }, acoes, naTelaInicial = true)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "︿  deslize para cima para ver todos",
            color = TextoFraco,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
