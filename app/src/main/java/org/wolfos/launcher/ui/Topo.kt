package org.wolfos.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.wolfos.launcher.dados.Cidade
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Previsao
import org.wolfos.launcher.dados.Tempo
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HM = DateTimeFormatter.ofPattern("HH:mm")

/** O desenho do tempo e como ele se mexe (o sol gira, a nuvem flutua, a chuva cai). */
private enum class Jeito { GIRA, FLUTUA, CAI, PARADO }

private fun desenhoDoTempo(codigo: Int, dia: Boolean): Pair<ImageVector, Jeito> = when (codigo) {
    0, 1 -> if (dia) Icons.Outlined.WbSunny to Jeito.GIRA else Icons.Outlined.NightsStay to Jeito.FLUTUA
    2 -> Icons.Outlined.WbCloudy to Jeito.FLUTUA
    3, 45, 48 -> Icons.Outlined.Cloud to Jeito.FLUTUA
    in 51..57 -> Icons.Outlined.Grain to Jeito.CAI
    in 61..67, in 80..82 -> Icons.Outlined.Umbrella to Jeito.CAI
    in 71..77, 85, 86 -> Icons.Outlined.AcUnit to Jeito.GIRA
    in 95..99 -> Icons.Outlined.FlashOn to Jeito.CAI
    else -> Icons.Outlined.WbCloudy to Jeito.PARADO
}

/** O icone do tempo, animado devagar. */
@Composable
fun IconeTempo(codigo: Int, dia: Boolean, tamanho: androidx.compose.ui.unit.Dp) {
    val (icone, jeito) = desenhoDoTempo(codigo, dia)
    val t = rememberInfiniteTransition(label = "tempo")
    val giro by t.animateFloat(0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "giro")
    val vai by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "vai")
    Icon(
        icone,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(tamanho)
            .graphicsLayer {
                when (jeito) {
                    Jeito.GIRA -> rotationZ = giro
                    Jeito.FLUTUA -> translationX = vai * 3.dp.toPx()
                    Jeito.CAI -> translationY = vai * 2.dp.toPx()
                    Jeito.PARADO -> Unit
                }
            },
    )
}

/** O clima do topo: icone animado, a temperatura e o resumo do dia. Tocar atualiza. */
@Composable
fun TempoAgora(p: Previsao, unidade: String, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = aoTocar).padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        IconeTempo(p.codigo, p.dia, 30.dp)
        Text(
            "${p.graus}$unidade",
            color = Texto, fontSize = 28.sp, fontWeight = FontWeight.Light,
            style = TextStyle(shadow = SombraTexto),
            modifier = Modifier.padding(start = 10.dp),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                Tempo.descricao(p.codigo) + " · " + p.cidade,
                color = Texto.copy(alpha = 0.9f), fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = SombraTexto),
            )
            Text(
                "máx ${p.maxima}$unidade · mín ${p.minima}$unidade" + if (p.chuva >= 20) " · chuva ${p.chuva}%" else "",
                color = Texto.copy(alpha = 0.6f), fontSize = 12.5.sp, maxLines = 1,
                style = TextStyle(shadow = SombraTexto),
            )
        }
    }
}

/** O que os compromissos do dia fazem. */
class AcoesCompromissos(
    val abrir: (Evento) -> Unit,
    val entrar: (Evento) -> Unit,
    val novo: () -> Unit,
    val permitir: () -> Unit,
)

/**
 * Os compromissos de hoje, da agenda do Android (a Google Agenda sincroniza nela):
 * hora, titulo e "Entrar" quando tem link de reuniao. Tocar abre o evento.
 */
@Composable
fun CompromissosDoDia(eventos: List<Evento>, temAgenda: Boolean, acoes: AcoesCompromissos, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    val agora = System.currentTimeMillis()
    val fimDoDia = org.wolfos.launcher.dados.Uso.inicioDoDia() + 24 * 3_600_000L
    val hoje = eventos.filter { it.inicio < fimDoDia && it.fim > agora }.take(3)
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Text("Hoje", color = Texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, style = TextStyle(shadow = SombraTexto))
            Box(Modifier.padding(start = 10.dp).width(20.dp).size(width = 20.dp, height = 2.dp).clip(CircleShape).background(cor))
            Box(Modifier.weight(1f))
            if (temAgenda) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = acoes.novo),
                ) { Icon(Icons.Outlined.Add, contentDescription = "Novo compromisso", tint = cor, modifier = Modifier.size(20.dp)) }
            }
        }
        when {
            !temAgenda -> LinhaCompromisso(null, "Ver os compromissos da Google Agenda", null, "Permitir", acoes.permitir, acoes.permitir)
            hoje.isEmpty() -> Text(
                "Nada marcado para hoje.",
                color = Texto.copy(alpha = 0.55f), fontSize = 13.5.sp,
                style = TextStyle(shadow = SombraTexto),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
            )
            else -> for (e in hoje) {
                val comecou = e.inicio <= agora
                LinhaCompromisso(
                    hora = when {
                        e.diaInteiro -> "dia"
                        comecou -> "agora"
                        else -> LocalDateTime.ofInstant(Instant.ofEpochMilli(e.inicio), ZoneId.systemDefault()).format(HM)
                    },
                    titulo = e.titulo,
                    sub = if (comecou || e.diaInteiro) null else quandoFalta(e.inicio, agora),
                    botao = if (e.link != null) "Entrar" else null,
                    aoBotao = { acoes.entrar(e) },
                    aoTocar = { acoes.abrir(e) },
                )
            }
        }
    }
}

@Composable
private fun LinhaCompromisso(hora: String?, titulo: String, sub: String?, botao: String?, aoBotao: () -> Unit, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = aoTocar)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
        if (hora != null) {
            Text(hora, color = cor, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).width(46.dp))
        }
        Column(Modifier.weight(1f).padding(start = if (hora == null) 10.dp else 0.dp)) {
            Text(titulo, color = Texto, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = SombraTexto))
            if (sub != null) Text(sub, color = Texto.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        if (botao != null) {
            Box(
                Modifier.clip(CircleShape).background(cor.copy(alpha = 0.2f)).clickable(onClick = aoBotao)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { Text(botao, color = cor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** A busca de cidade do clima (nos ajustes). */
@Composable
fun EscolherCidade(aoEscolher: (Cidade) -> Unit, aoFechar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    val escopo = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    var achadas by remember { mutableStateOf<List<Cidade>?>(null) }
    val buscar = { escopo.launch { achadas = Tempo.buscarCidades(texto) }; Unit }
    Dialog(onDismissRequest = aoFechar) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Superficie)
                .padding(vertical = 18.dp),
        ) {
            Text("Cidade do clima", color = Texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Texto, fontSize = 15.sp),
                    cursorBrush = SolidColor(cor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { buscar() }),
                    decorationBox = { campo ->
                        Box {
                            if (texto.isEmpty()) Text("Ex.: Piracicaba", color = TextoFraco, fontSize = 15.sp)
                            campo()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Fundo.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
                Box(Modifier.padding(start = 8.dp)) { Pilula("Buscar", destaque = true) { buscar() } }
            }
            when {
                achadas == null -> Unit
                achadas!!.isEmpty() -> Text("Nenhuma cidade com esse nome.", color = TextoFraco, modifier = Modifier.padding(horizontal = 22.dp))
                else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(achadas!!) { c ->
                        Column(Modifier.fillMaxWidth().clickable { aoEscolher(c) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                            Text(c.nome, color = Texto, fontSize = 16.sp)
                            Text(c.regiao, color = TextoFraco, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}
