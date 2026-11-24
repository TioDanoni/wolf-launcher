package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.DiaPrevisto
import org.wolfos.launcher.dados.Previsao
import org.wolfos.launcher.dados.Semana
import org.wolfos.launcher.dados.Tempo
import org.wolfos.launcher.dados.semana
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")
private val DIA_SEMANA = DateTimeFormatter.ofPattern("EEE", PT)
private val HH = DateTimeFormatter.ofPattern("HH")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/** As cores do frio ao calor, para a barrinha de cada dia. */
private val FRIO = Color(0xFF6FB7FF)
private val CALOR = Color(0xFFFF8A4C)

/**
 * A previsao da semana, numa folha que sobe de baixo com o topo tingido pela cor do
 * tempo de agora: o tempo atual grande, nascer e por do sol, as proximas horas numa
 * fileira que rola e os 7 dias, cada um com a barrinha da minima a maxima (em relacao a
 * semana inteira, do azul do frio ao laranja do calor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrevisaoSemana(agora: Previsao, unidade: String, aoFechar: () -> Unit) {
    val contexto = LocalContext.current
    val semana by produceState<Semana?>(null) { value = Tempo.semana(contexto) }
    val corTempo = corDoTempo(agora.codigo, agora.dia)
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Superficie,
        contentColor = Texto,
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to corTempo.copy(alpha = 0.22f), 0.45f to Color.Transparent))
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            // agora
            Text(agora.cidade, color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(start = 24.dp, top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                IconeTempo(agora.codigo, agora.dia, 56.dp)
                Text("${agora.graus}$unidade", color = Texto, fontSize = 64.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(start = 16.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(Tempo.descricao(agora.codigo), color = Texto, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("máx ${agora.maxima}$unidade · mín ${agora.minima}$unidade", color = TextoFraco, fontSize = 13.5.sp)
                }
            }
            semana?.let { s ->
                if (s.nascer != null && s.por != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 6.dp)) {
                        Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = Color(0xFFFFC94A), modifier = Modifier.size(16.dp))
                        Text("  nasce ${s.nascer.format(HHMM)}", color = TextoFraco, fontSize = 13.sp)
                        Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = Lilas, modifier = Modifier.padding(start = 18.dp).size(16.dp))
                        Text("  se põe ${s.por.format(HHMM)}", color = TextoFraco, fontSize = 13.sp)
                    }
                }
            }

            when (val s = semana) {
                null -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = corTempo, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                else -> {
                    SecaoMenu("Próximas horas")
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(s.horas) { i, h ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(58.dp)
                                    .clip(CircleShape)
                                    .background(if (i == 0) corTempo.copy(alpha = 0.14f) else Color.Transparent)
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(if (i == 0) "Agora" else h.hora.format(HH) + "h", color = if (i == 0) Texto else TextoFraco, fontSize = 12.5.sp)
                                Box(Modifier.padding(vertical = 8.dp)) { IconeTempo(h.codigo, h.dia, 26.dp) }
                                Text("${h.graus}°", color = Texto, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(if (h.chuva >= 20) "${h.chuva}%" else " ", color = FRIO, fontSize = 11.sp)
                            }
                        }
                    }

                    SecaoMenu("7 dias")
                    val minSemana = s.dias.minOfOrNull { it.minima } ?: 0
                    val maxSemana = s.dias.maxOfOrNull { it.maxima } ?: 1
                    s.dias.forEachIndexed { i, d -> LinhaDia(d, i, minSemana, maxSemana, unidade) }
                    Text(
                        "Previsão do Open-Meteo",
                        color = TextoFraco.copy(alpha = 0.6f), fontSize = 11.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 12.dp),
                    )
                }
            }
        }
    }
}

/** Um dia: o nome, o icone, a chuva, a minima, a barrinha e a maxima. */
@Composable
private fun LinhaDia(d: DiaPrevisto, i: Int, minSemana: Int, maxSemana: Int, unidade: String) {
    val nome = when {
        d.data == LocalDate.now() -> "Hoje"
        d.data == LocalDate.now().plusDays(1) -> "Amanhã"
        else -> d.data.format(DIA_SEMANA).replace(".", "").replaceFirstChar { it.uppercase() }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().entrada(i, atraso = 45L).padding(horizontal = 24.dp, vertical = 9.dp),
    ) {
        Text(nome, color = Texto, fontSize = 15.sp, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(72.dp))
        IconeTempo(d.codigo, true, 24.dp)
        Text(if (d.chuva >= 20) "${d.chuva}%" else "", color = FRIO, fontSize = 12.sp, modifier = Modifier.width(44.dp).padding(start = 8.dp))
        Text("${d.minima}°", color = TextoFraco, fontSize = 15.sp, modifier = Modifier.width(36.dp))
        // a barrinha: a faixa do dia dentro da faixa da semana
        BoxWithConstraints(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Texto.copy(alpha = 0.10f))) {
            val total = (maxSemana - minSemana).coerceAtLeast(1).toFloat()
            val ini = (d.minima - minSemana) / total
            val fim = (d.maxima - minSemana) / total
            val frioDoDia = lerpCor(ini)
            val calorDoDia = lerpCor(fim)
            Box(
                Modifier
                    .offset(x = maxWidth * ini)
                    .width(maxWidth * (fim - ini).coerceAtLeast(0.06f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(frioDoDia, calorDoDia))),
            )
        }
        Text("${d.maxima}$unidade", color = Texto, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp).width(44.dp))
    }
}

/** Do azul (frio) ao laranja (calor). */
private fun lerpCor(f: Float): Color = androidx.compose.ui.graphics.lerp(FRIO, CALOR, f.coerceIn(0f, 1f))
