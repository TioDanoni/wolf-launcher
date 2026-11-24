package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Insights
import org.wolfos.launcher.dados.Nota
import org.wolfos.launcher.dados.ResumoDia
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
private val DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT)

/** Tudo o que o Seu dia mostra. */
class EstadoDia(
    /** O resumo de hoje (nulo sem o acesso ao uso). */
    val resumo: ResumoDia?,
    val temUso: Boolean,
    val notas: List<Nota>,
    /** Os apps sugeridos para agora (Wolf Sense). */
    val sugeridos: List<AppInfo>,
    /** Para achar o icone e o nome de cada pacote das sessoes. */
    val porPacote: Map<String, AppInfo>,
    val eventos: List<Evento>,
    val alarme: Long?,
    val temAgenda: Boolean,
    val modoNoite: Boolean,
    /** Quando o modo noite comeca (minutos do dia). */
    val noiteInicio: Int,
    /** Quantas notificacoes estao esperando. */
    val avisos: Int,
)

/** O que a tela do dia pede para a tela principal fazer. */
class AcoesDia(
    val abrirApp: (AppInfo) -> Unit,
    val abrirEvento: (Evento) -> Unit,
    val entrarEvento: (Evento) -> Unit,
    val abrirAlarmes: () -> Unit,
    val permitirAgenda: () -> Unit,
    val permitirUso: () -> Unit,
    val abrirInsights: () -> Unit,
    val abrirPainel: () -> Unit,
    val voltar: () -> Unit,
)

/**
 * Seu dia: o resumo (tempo de tela, desbloqueios, tarefas) e a linha do tempo em tres
 * momentos. Antes: quando o dia comecou e as sessoes de uso, agrupadas (nao cada app
 * aberto). Agora: a saudacao, os apps sugeridos e o que esta esperando. Depois: agenda,
 * tarefas, despertador e o modo noite.
 */
@Composable
fun DiaTela(estado: EstadoDia, acoes: AcoesDia) {
    val agora = System.currentTimeMillis()
    val hoje = LocalDateTime.now()
    AreaDeGestos(
        aoDeslizar = { dx, dy -> if (direcao(dx, dy) == 0 && dx < 0) acoes.voltar() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                Text("Seu dia", color = Texto, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text(hoje.format(DATA_LONGA).replaceFirstChar { it.uppercase() }, color = TextoFraco, fontSize = 14.sp)
            }
            Resumo(estado, acoes)

            // ---- antes
            val r = estado.resumo
            var i = 0
            if (r?.primeiroDesbloqueio != null) {
                Marco(
                    Icons.Outlined.WbTwilight, hora(r.primeiroDesbloqueio), parteDoDia(r.primeiroDesbloqueio),
                    "Você começou seu dia", "primeiro desbloqueio", i++, apagado = true,
                )
            }
            // so as sessoes que contam (3 min ou mais), as 4 mais recentes
            r?.sessoes?.filter { it.minutos >= 3 }?.takeLast(4)?.forEach { s ->
                val apps = s.pacotes.mapNotNull { estado.porPacote[it] }
                Marco(
                    Icons.Outlined.PhoneAndroid, hora(s.inicio), "ATIVIDADE",
                    "Sessão de ${Insights.duracao(s.fim - s.inicio)}",
                    when (apps.size) {
                        0 -> null
                        1 -> apps.first().nome
                        else -> "${apps.size} aplicativos"
                    },
                    i++, apagado = true, apps = apps.take(4), aoApp = acoes.abrirApp,
                )
            }
            if (!estado.temUso) {
                Marco(
                    Icons.Outlined.Insights, "", "ANTES", "Veja como foi o seu dia",
                    "tempo de tela, sessões e apps mais usados (fica só no celular)", i++,
                    botao = "Permitir", aoBotao = acoes.permitirUso,
                )
            }

            // ---- agora
            Agora(estado, acoes)

            // ---- depois
            estado.eventos.filter { it.fim > agora }.take(4).forEach { e ->
                val comecou = e.inicio <= agora
                Marco(
                    Icons.Outlined.Event,
                    if (e.diaInteiro) "dia" else hora(e.inicio),
                    if (comecou) "AGORA" else "PRÓXIMO",
                    e.titulo,
                    if (comecou) "acontecendo" else quandoFalta(e.inicio, agora),
                    i++,
                    botao = if (e.link != null) "Entrar" else null,
                    aoBotao = { acoes.entrarEvento(e) },
                ) { acoes.abrirEvento(e) }
            }
            if (!estado.temAgenda) {
                Marco(Icons.Outlined.Event, "", "DEPOIS", "Sua agenda aqui", "reuniões e lembretes", i++, botao = "Permitir", aoBotao = acoes.permitirAgenda)
            }
            val pendentes = estado.notas.filter { it.tarefa && !it.feita }
            if (pendentes.isNotEmpty()) {
                Marco(
                    Icons.Outlined.CheckCircle, "", "TAREFAS",
                    if (pendentes.size == 1) pendentes.first().texto else "${pendentes.size} tarefas pendentes",
                    pendentes.drop(if (pendentes.size == 1) 1 else 0).take(2).joinToString(" · ") { it.texto }.ifEmpty { null },
                    i++,
                ) { acoes.abrirPainel() }
            }
            estado.alarme?.let { a ->
                Marco(Icons.Outlined.Alarm, hora(a), "DESPERTADOR", "Despertador", "toca ${quandoFalta(a, agora)}", i++) { acoes.abrirAlarmes() }
            }
            if (estado.modoNoite) {
                val h = "%02d:%02d".format(estado.noiteInicio / 60, estado.noiteInicio % 60)
                Marco(Icons.Outlined.Bedtime, h, "NOITE", "Modo descanso", "cores calmas e menos distração", i++, ultimo = true)
            }

            if (estado.temUso) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 18.dp, top = 18.dp)
                        .clip(CircleShape)
                        .clickable(onClick = acoes.abrirInsights)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Ver a semana (Wolf Insights)", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Tempo de tela, desbloqueios e tarefas, lado a lado. */
@Composable
private fun Resumo(estado: EstadoDia, acoes: AcoesDia) {
    val r = estado.resumo
    val tarefas = estado.notas.filter { it.tarefa }
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Numero("Tempo de tela", r?.let { Insights.duracao(it.tempoDeTela) } ?: "—", Modifier.weight(1f).clickable(enabled = r != null, onClick = acoes.abrirInsights))
        Numero("Desbloqueios", r?.desbloqueios?.toString() ?: "—", Modifier.weight(1f))
        Numero(
            "Tarefas", if (tarefas.isEmpty()) "—" else "${tarefas.count { it.feita }}/${tarefas.size}",
            Modifier.weight(1f).clickable(onClick = acoes.abrirPainel),
        )
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Text(rotulo, color = TextoFraco, fontSize = 12.5.sp)
        Text(valor, color = Texto, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Um marco da linha do tempo: o icone na linha (na cor do tema), "07:30 · MANHÃ" em
 * cima, o titulo e o detalhe; opcionalmente os icones dos apps ou um botao.
 */
@Composable
private fun Marco(
    icone: ImageVector,
    hora: String,
    rotulo: String,
    titulo: String,
    sub: String?,
    indice: Int,
    apagado: Boolean = false,
    apps: List<AppInfo> = emptyList(),
    aoApp: (AppInfo) -> Unit = {},
    botao: String? = null,
    aoBotao: () -> Unit = {},
    ultimo: Boolean = false,
    aoTocar: (() -> Unit)? = null,
) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .entrada(indice.coerceAtMost(10), atraso = 40L)
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(start = 20.dp, end = 20.dp),
    ) {
        // a linha do rio com o icone do marco
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp).fillMaxHeight()) {
            Icon(icone, contentDescription = null, tint = if (apagado) TextoFraco else cor, modifier = Modifier.padding(top = 2.dp).size(18.dp))
            if (!ultimo) Box(Modifier.padding(top = 4.dp).width(1.dp).weight(1f).background(Texto.copy(alpha = 0.15f)))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, bottom = 18.dp)) {
            Text(
                listOf(hora, rotulo).filter { it.isNotBlank() }.joinToString(" · "),
                color = if (apagado) TextoFraco else Texto.copy(alpha = 0.8f), fontSize = 12.sp, letterSpacing = 0.06.em,
            )
            Text(
                titulo, color = if (apagado) Texto.copy(alpha = 0.7f) else Texto, fontSize = 16.5.sp,
                fontWeight = if (apagado) FontWeight.Normal else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (apps.isNotEmpty() || sub != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    apps.forEach { a ->
                        IconeApp(a, 20.dp, Modifier.padding(end = 6.dp).clip(RoundedCornerShape(6.dp)).clickable { aoApp(a) })
                    }
                    if (sub != null) Text(sub, color = TextoFraco, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = if (apps.isEmpty()) 0.dp else 4.dp))
                }
            }
            if (botao != null) {
                Text(
                    botao, color = cor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    style = TextStyle(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = aoBotao),
                )
            }
        }
    }
}

/** O AGORA: a hora, a saudacao, os apps sugeridos (Wolf Sense) e o que esta esperando. */
@Composable
private fun Agora(estado: EstadoDia, acoes: AcoesDia) {
    val cor = MaterialTheme.colorScheme.primary
    val agora = LocalDateTime.now()
    val saudacao = when (agora.hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 20.dp, end = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp).fillMaxHeight()) {
            Box(Modifier.padding(top = 6.dp).size(14.dp).clip(CircleShape).background(Brush.linearGradient(listOf(cor, MaterialTheme.colorScheme.secondary))))
            Box(Modifier.padding(top = 4.dp).width(1.dp).weight(1f).background(Texto.copy(alpha = 0.15f)))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Brush.linearGradient(listOf(cor.copy(alpha = 0.45f), Color.Transparent)), RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(agora.format(HH_MM), color = Texto, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text("  AGORA", color = cor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(saudacao, color = Texto, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            if (estado.avisos > 0) {
                Text("${estado.avisos} notificação(ões) esperando", color = cor, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (estado.sugeridos.isNotEmpty()) {
                Text("Para este momento", color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    estado.sugeridos.take(4).forEach { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp).clip(RoundedCornerShape(14.dp)).clickable { acoes.abrirApp(app) },
                        ) {
                            IconeApp(app, 36.dp)
                            Text(
                                app.nome, color = Texto.copy(alpha = 0.8f), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hora(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(HH_MM)

private fun parteDoDia(millis: Long): String = when (LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).hour) {
    in 5..11 -> "MANHÃ"
    in 12..17 -> "TARDE"
    else -> "NOITE"
}

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
