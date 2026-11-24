package org.wolfos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Insights
import org.wolfos.launcher.dados.ResumoDia
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DIA_CURTO = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("pt-BR"))

/**
 * Wolf Insights: a semana de uso. O tempo de tela de cada dia (uma cor so, tocar numa
 * barra mostra o valor), a media, a comparacao com a semana anterior, o horario de
 * pico e os apps que mais tomam tempo. Tudo lido e calculado no proprio celular.
 */
@Composable
fun InsightsFolha(porPacote: Map<String, AppInfo>, aoFechar: () -> Unit) {
    val contexto = LocalContext.current
    // 14 dias: a semana de agora e a anterior, para comparar
    val dias by produceState<List<ResumoDia>?>(null) {
        value = withContext(Dispatchers.IO) { Insights.semana(contexto, 14) }
    }
    Folha(aoFechar) {
        Text("Wolf Insights", color = Texto, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Text("Como você usou o celular nos últimos 7 dias", color = TextoFraco, fontSize = 13.5.sp, modifier = Modifier.padding(horizontal = 24.dp))
        val d = dias
        if (d == null) {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            return@Folha
        }
        val semana = d.takeLast(7)
        val anterior = d.dropLast(7)
        val total = semana.sumOf { it.tempoDeTela }
        val media = if (semana.isEmpty()) 0L else total / semana.size
        val totalAnterior = anterior.sumOf { it.tempoDeTela }

        // os numeros principais
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Grande("Média por dia", Insights.duracao(media), Modifier.weight(1f))
            val variacao = if (totalAnterior > 0) ((total - totalAnterior) * 100 / totalAnterior).toInt() else null
            Grande(
                "Contra a semana anterior",
                variacao?.let { (if (it > 0) "+" else "") + "$it%" } ?: "—",
                Modifier.weight(1f),
                variacao?.let { if (it > 0) "mais tempo de tela" else "menos tempo de tela" },
            )
        }

        Barras(semana)

        // o horario de pico
        val porHora = LongArray(24).also { soma -> semana.forEach { r -> r.porHora.forEachIndexed { h, v -> soma[h] += v } } }
        val pico = porHora.indices.maxByOrNull { porHora[it] }
        if (pico != null && porHora[pico] > 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                Grande("Horário de pico", "${pico}h – ${(pico + 1) % 24}h", Modifier.weight(1f))
                Grande("Desbloqueios por dia", (semana.sumOf { it.desbloqueios } / semana.size.coerceAtLeast(1)).toString(), Modifier.weight(1f))
            }
        }

        // os apps que mais tomam tempo
        val apps = HashMap<String, Long>().also { m -> semana.forEach { r -> r.porApp.forEach { (p, t) -> m[p] = (m[p] ?: 0L) + t } } }
        val top = apps.entries.sortedByDescending { it.value }.take(5)
        if (top.isNotEmpty()) {
            SecaoMenu("Apps que mais tomam seu tempo")
            val maior = top.first().value.coerceAtLeast(1)
            top.forEach { (pacote, tempo) ->
                val app = porPacote[pacote]
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 7.dp)) {
                    if (app != null) IconeApp(app, 30.dp) else Box(Modifier.size(30.dp))
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Row {
                            Text(app?.nome ?: pacote, color = Texto, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(Insights.duracao(tempo), color = TextoFraco, fontSize = 13.5.sp)
                        }
                        // barra fina, uma cor so (a mesma das barras da semana)
                        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Texto.copy(alpha = 0.10f))) {
                            Box(Modifier.fillMaxWidth(tempo.toFloat() / maior).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
        }
        Text(
            "Lido do próprio Android; nada sai do celular.",
            color = TextoFraco.copy(alpha = 0.7f), fontSize = 11.5.sp, modifier = Modifier.padding(start = 24.dp, top = 14.dp),
        )
    }
}

@Composable
private fun Grande(rotulo: String, valor: String, modifier: Modifier, sub: String? = null) {
    Column(modifier) {
        Text(rotulo, color = TextoFraco, fontSize = 12.5.sp)
        Text(valor, color = Texto, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        if (sub != null) Text(sub, color = TextoFraco, fontSize = 12.sp)
    }
}

/**
 * O tempo de tela de cada dia: uma serie, uma cor (a da paleta), barras finas com o
 * topo arredondado saindo da base, 2 dp entre elas. O dia escolhido (hoje, ou o que a
 * pessoa tocar) fica forte e mostra o valor em cima; os outros, mais claros.
 */
@Composable
private fun Barras(dias: List<ResumoDia>) {
    val cor = MaterialTheme.colorScheme.primary
    var escolhido by remember { mutableIntStateOf(dias.lastIndex) }
    val maior = (dias.maxOfOrNull { it.tempoDeTela } ?: 1L).coerceAtLeast(1L)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 24.dp),
    ) {
        dias.forEachIndexed { i, r ->
            val alto by animateFloatAsState(r.tempoDeTela.toFloat() / maior, tween(500 + i * 60), label = "barra$i")
            val marcado = i == escolhido
            // a area de toque e a coluna inteira (maior que a barra)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { escolhido = i },
            ) {
                Text(
                    if (marcado) Insights.duracao(r.tempoDeTela) else "",
                    color = Texto, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Box(
                    Modifier
                        .width(18.dp)
                        .fillMaxHeight(fraction = (alto * 0.78f).coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (marcado) cor else cor.copy(alpha = 0.45f)),
                )
            }
        }
    }
    // a base e os dias
    Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(Texto.copy(alpha = 0.15f)))
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        dias.forEachIndexed { i, r ->
            Text(
                if (i == dias.lastIndex) "hoje" else r.data.format(DIA_CURTO).replace(".", ""),
                color = if (i == escolhido) Texto else TextoFraco, fontSize = 11.5.sp,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
