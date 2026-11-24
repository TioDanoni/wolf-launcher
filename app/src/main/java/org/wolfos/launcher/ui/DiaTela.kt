package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Evento
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
private val DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

/** Algo que aconteceu hoje: um app aberto ou uma notificacao. */
sealed interface Passado {
    val quando: Long

    data class Abriu(override val quando: Long, val app: AppInfo) : Passado
    data class Recebeu(override val quando: Long, val aviso: Aviso, val app: AppInfo?) : Passado
}

/** O que a tela do dia pede para a tela principal fazer. */
class AcoesDia(
    val abrirApp: (AppInfo) -> Unit,
    val abrirAviso: (Aviso) -> Unit,
    val abrirEvento: (Evento) -> Unit,
    val entrarEvento: (Evento) -> Unit,
    val abrirAlarmes: () -> Unit,
    val permitirAgenda: () -> Unit,
    val buscar: () -> Unit,
    val voltar: () -> Unit,
)

/**
 * Seu dia, como um rio: o que passou em cima, o agora no meio (com os apps que voce
 * costuma abrir nessa hora) e o que vem embaixo (agenda e despertador).
 */
@Composable
fun DiaTela(
    antes: List<Passado>,
    costume: List<AppInfo>,
    depois: List<Evento>,
    alarme: Long?,
    temAgenda: Boolean,
    acoes: AcoesDia,
) {
    val agoraMs = System.currentTimeMillis()
    val linhaCor = Texto.copy(alpha = 0.12f)
    // abre com o AGORA perto do meio da tela
    val estado = rememberLazyListState(initialFirstVisibleItemIndex = (antes.size - 2).coerceAtLeast(0))

    AreaDeDesenho(
        aoDeslizar = { dx, dy -> if (direcao(dx, dy) == 0 && dx < 0) acoes.voltar() },
        aoLetra = { _, _ -> },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = estado,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                // a linha do rio, atras de tudo
                .drawBehind {
                    val x = 86.dp.toPx()
                    drawLine(linhaCor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                },
        ) {
            item { Spacer(Modifier.height(24.dp)) }
            item { Secao("Antes") }
            if (antes.isEmpty()) item { Vazio("Nada por aqui ainda hoje.") }
            antes.forEach { p ->
                item {
                    when (p) {
                        is Passado.Abriu -> Momento(hora(p.quando), "Você abriu ${p.app.nome}", null, apagado = true, app = p.app) {
                            acoes.abrirApp(p.app)
                        }
                        is Passado.Recebeu -> Momento(
                            hora(p.quando),
                            p.aviso.titulo.ifEmpty { p.app?.nome.orEmpty() },
                            p.aviso.texto,
                            apagado = true,
                            app = p.app,
                        ) { acoes.abrirAviso(p.aviso) }
                    }
                }
            }
            item { Agora(agoraMs, costume, acoes) }
            item { Secao("Depois", destaque = true) }
            alarme?.let { a ->
                item { Momento(hora(a), "Despertador", "toca ${quandoFalta(a, agoraMs)}", botao = "Ver", aoBotao = acoes.abrirAlarmes) { acoes.abrirAlarmes() } }
            }
            depois.forEach { e ->
                item {
                    val comecou = e.inicio <= agoraMs
                    Momento(
                        if (e.diaInteiro) "dia" else hora(e.inicio),
                        e.titulo,
                        listOfNotNull(
                            if (comecou) "agora" else quandoFalta(e.inicio, agoraMs),
                            e.local.takeIf { it.isNotBlank() && e.link == null },
                        ).joinToString(" · "),
                        botao = if (e.link != null) "Entrar" else null,
                        aoBotao = { acoes.entrarEvento(e) },
                    ) { acoes.abrirEvento(e) }
                }
            }
            if (!temAgenda) {
                item {
                    Momento("", "Ver a sua agenda aqui", "reuniões, aniversários e lembretes", botao = "Permitir", aoBotao = acoes.permitirAgenda) {
                        acoes.permitirAgenda()
                    }
                }
            } else if (depois.isEmpty() && alarme == null) {
                item { Vazio("Nada marcado nas próximas 24 h.") }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun Secao(titulo: String, destaque: Boolean = false) {
    Rotulo(
        titulo,
        Modifier.padding(start = 106.dp, top = 16.dp, bottom = 8.dp),
        if (destaque) MaterialTheme.colorScheme.primary else TextoFraco,
    )
}

@Composable
private fun Vazio(texto: String) {
    Text(texto, color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(start = 106.dp, top = 4.dp, bottom = 12.dp))
}

/** Uma linha do rio: hora, o ponto na linha, o que foi e (talvez) um botao ou o icone. */
@Composable
private fun Momento(
    hora: String,
    titulo: String,
    sub: String?,
    apagado: Boolean = false,
    app: AppInfo? = null,
    botao: String? = null,
    aoBotao: () -> Unit = {},
    aoTocar: () -> Unit,
) {
    val cor = MaterialTheme.colorScheme.primary
    val alfa = if (apagado) 0.6f else 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(vertical = 10.dp),
    ) {
        Text(hora, color = TextoFraco.copy(alpha = 0.5f * alfa + 0.2f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(72.dp).padding(start = 24.dp))
        Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Texto.copy(alpha = 0.4f * alfa)))
        }
        Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
            Text(titulo, color = Texto.copy(alpha = alfa), fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) {
                Text(sub, color = TextoFraco.copy(alpha = 0.6f * alfa + 0.2f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (botao != null) {
            Box(
                Modifier
                    .padding(end = 18.dp)
                    .clip(CircleShape)
                    .background(cor.copy(alpha = 0.18f))
                    .border(1.dp, cor, CircleShape)
                    .clickable(onClick = aoBotao)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) { Text(botao, color = cor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        } else if (app != null) {
            IconeApp(app, 30.dp, Modifier.padding(end = 20.dp))
        }
    }
}

/** O AGORA: o relogio, o dia e os apps que voce costuma abrir nessa hora. */
@Composable
private fun Agora(agoraMs: Long, costume: List<AppInfo>, acoes: AcoesDia) {
    val cor = MaterialTheme.colorScheme.primary
    val momento = LocalDateTime.ofInstant(Instant.ofEpochMilli(agoraMs), ZoneId.systemDefault())
    val forma = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(forma)
            .background(Superficie.copy(alpha = 0.92f))
            .border(1.dp, cor.copy(alpha = 0.4f), forma)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(momento.format(HH_MM), color = Texto, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight)
            Column(Modifier.padding(start = 16.dp)) {
                Rotulo("Agora", cor = cor)
                Text(momento.format(DATA_LONGA).replaceFirstChar { it.uppercase() }, color = TextoFraco, fontSize = 13.sp)
            }
        }
        if (costume.isNotEmpty()) {
            Rotulo("Você costuma abrir nessa hora", Modifier.padding(top = 16.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                costume.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(62.dp).clip(RoundedCornerShape(14.dp)).clickable { acoes.abrirApp(app) },
                    ) {
                        IconeApp(app, 44.dp)
                        Text(app.nome, color = Texto.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = acoes.buscar)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) { Text("⌕  todos os apps, pessoas, ajustes…", color = TextoFraco, fontSize = 13.sp) }
    }
}

private fun hora(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(HH_MM)

/** "em 28 min", "em 2 h 10", "amanhã 06:30". */
fun quandoFalta(alvo: Long, agora: Long): String {
    val min = ((alvo - agora) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "agora"
        min < 60 -> "em $min min"
        min < 12 * 60 -> "em ${min / 60} h" + if (min % 60 > 0) " ${min % 60}" else ""
        else -> {
            val dia = LocalDateTime.ofInstant(Instant.ofEpochMilli(alvo), ZoneId.systemDefault()).toLocalDate()
            val hoje = LocalDateTime.ofInstant(Instant.ofEpochMilli(agora), ZoneId.systemDefault()).toLocalDate()
            (if (dia == hoje) "hoje " else "amanhã ") + hora(alvo)
        }
    }
}
