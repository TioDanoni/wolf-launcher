package org.wolfos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.wolfos.launcher.avisos.Musica
import org.wolfos.launcher.dados.Aparelho
import org.wolfos.launcher.dados.Armazenamento
import org.wolfos.launcher.dados.Bateria
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Lanterna
import org.wolfos.launcher.dados.Nota
import org.wolfos.launcher.dados.PartePainel
import org.wolfos.launcher.dados.Previsao
import org.wolfos.launcher.dados.Tempo
import org.wolfos.launcher.dados.TimerWolf
import org.wolfos.launcher.dados.WidgetSalvo
import org.wolfos.launcher.dados.interpretar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tudo o que o Painel mostra. */
class EstadoPainel(
    val previsao: Previsao?,
    val unidade: String,
    val aparelhos: List<Aparelho>,
    val temBluetooth: Boolean,
    val timer: TimerWolf,
    /** Muda quando o timer muda (a tela le de novo). */
    val timerMudou: Int,
    val musica: Musica?,
    val notas: List<Nota>,
    val eventos: List<Evento>,
    val temAgenda: Boolean,
    val widgets: List<WidgetSalvo>,
    val ordem: List<PartePainel>,
    val escondidas: Set<PartePainel>,
)

/** O que o Painel faz. */
class AcoesPainel(
    val voltar: () -> Unit,
    val abrirClima: () -> Unit,
    val pedirBluetooth: () -> Unit,
    val scanner: () -> Unit,
    val comecarTimer: (Int, String) -> Unit,
    val pararTimer: () -> Unit,
    val alternarCrono: () -> Unit,
    val zerarCrono: () -> Unit,
    val acoesPlayer: AcoesPlayer,
    val adicionarNota: (String, Boolean) -> Unit,
    val alternarNota: (Long) -> Unit,
    val apagarNota: (Long) -> Unit,
    val limparFeitas: () -> Unit,
    val abrirEvento: (Evento) -> Unit,
    val entrarEvento: (Evento) -> Unit,
    val permitirAgenda: () -> Unit,
    val acoesWidgets: AcoesWidgets,
    val moverParte: (PartePainel, Int) -> Unit,
    val alternarParte: (PartePainel) -> Unit,
)

private val HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * O Painel (deslizar para a esquerda): a central de ferramentas. Clima e bateria,
 * controles rapidos (lanterna, timer, scanner, calculadora), o timer correndo, a musica,
 * notas e tarefas, os proximos compromissos, o armazenamento e os widgets do Android.
 * Cada parte pode ser reordenada ou escondida em "Personalizar painel".
 */
@Composable
fun PainelTela(estado: EstadoPainel, acoes: AcoesPainel) {
    var personalizando by remember { mutableStateOf(false) }
    var escolhendoTimer by remember { mutableStateOf(false) }
    AreaDeGestos(
        aoDeslizar = { dx, dy -> if (direcao(dx, dy) == 0 && dx > 0) acoes.voltar() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Painel", color = Texto, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Text("Seu celular. Seu ritmo. Sua matilha.", color = TextoFraco, fontSize = 12.5.sp)
                }
                BotaoIcone(Icons.Outlined.Tune, "Personalizar painel") { personalizando = true }
            }
            var i = 0
            for (parte in estado.ordem) {
                if (parte in estado.escondidas) continue
                val visivel = when (parte) {
                    PartePainel.MUSICA -> estado.musica != null
                    PartePainel.TIMER -> estado.timer.fim > 0 || estado.timer.cronoInicio > 0 || estado.timer.cronoAcumulado > 0
                    else -> true
                }
                if (!visivel) continue
                key(parte) {
                    Box(Modifier.entrada(i++, atraso = 45L)) {
                        when (parte) {
                            PartePainel.CLIMA_BATERIA -> ClimaBateria(estado, acoes)
                            PartePainel.CONTROLES -> Controles(acoes) { escolhendoTimer = true }
                            PartePainel.TIMER -> TimerCorrendo(estado, acoes)
                            PartePainel.MUSICA -> estado.musica?.let { m ->
                                Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) { Player(m, acoes.acoesPlayer) }
                            }
                            PartePainel.NOTAS -> NotasRapidas(estado.notas, acoes)
                            PartePainel.AGENDA -> Proximos(estado, acoes)
                            PartePainel.ARMAZENAMENTO -> Espaco()
                            PartePainel.WIDGETS -> WidgetsDoAndroid(estado.widgets, acoes.acoesWidgets)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 18.dp)
                    .clip(CircleShape)
                    .clickable { personalizando = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Personalizar painel", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (personalizando) Personalizar(estado, acoes) { personalizando = false }
    if (escolhendoTimer) EscolherTimer(estado.timer, acoes) { escolhendoTimer = false }
}

// ---------------------------------------------------------------- pecas comuns

@Composable
private fun Titulo(texto: String, modifier: Modifier = Modifier, fim: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 22.dp, bottom = 8.dp)) {
        Text(
            texto.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.12.em, modifier = Modifier.weight(1f),
        )
        fim?.invoke()
    }
}

@Composable
private fun BotaoIcone(icone: ImageVector, descricao: String, cor: Color = MaterialTheme.colorScheme.primary, aoTocar: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = aoTocar),
    ) { Icon(icone, contentDescription = descricao, tint = cor, modifier = Modifier.size(22.dp)) }
}

/** Uma barra fina de progresso, no degrade da paleta. */
@Composable
private fun Barra(parte: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(parte.coerceIn(0f, 1f), label = "barra")
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Texto.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(p).height(4.dp).clip(CircleShape).background(degradeH()))
    }
}

/** Le de novo a cada intervalo, enquanto o Painel esta aberto. */
@Composable
private fun <T> aCada(ms: Long, chave: Any? = null, ler: () -> T): T {
    val v by produceState(ler(), chave) {
        while (true) {
            value = ler()
            delay(ms)
        }
    }
    return v
}

// ---------------------------------------------------------------- clima e bateria

@Composable
private fun ClimaBateria(estado: EstadoPainel, acoes: AcoesPainel) {
    val contexto = LocalContext.current
    val bateria = aCada(30_000) { Bateria.estado(contexto) }
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
        // clima (tocar abre a previsao da semana)
        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = acoes.abrirClima).padding(end = 12.dp)) {
            val p = estado.previsao
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p != null) IconeTempo(p.codigo, p.dia, 20.dp)
                Text(p?.cidade ?: "Clima", color = TextoFraco, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            }
            if (p != null) {
                Text("${p.graus}${estado.unidade}", color = Texto, fontSize = 34.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 6.dp))
                Text(Tempo.descricao(p.codigo), color = Texto.copy(alpha = 0.85f), fontSize = 13.5.sp, maxLines = 1)
                Text(
                    "Máx ${p.maxima}° · Mín ${p.minima}°" + if (p.chuva >= 20) " · chuva ${p.chuva}%" else "",
                    color = TextoFraco, fontSize = 12.5.sp, maxLines = 1,
                )
            } else {
                Text("Escolha a cidade nos ajustes", color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        // bateria: celular e os aparelhos conectados
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            val (pct, carregando) = bateria ?: (0 to false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (carregando) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                )
                Text("Bateria", color = TextoFraco, fontSize = 13.5.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text("$pct%", color = if (pct <= 15 && !carregando) Perigo else Texto, fontSize = 34.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 6.dp))
            Barra(pct / 100f, Modifier.padding(vertical = 4.dp))
            Text(if (carregando) "Carregando" else "Na bateria", color = TextoFraco, fontSize = 12.5.sp)
            for (a in estado.aparelhos.take(2)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = TextoFraco, modifier = Modifier.size(14.dp))
                    Text(
                        a.nome + (a.bateria?.let { " · $it%" } ?: ""), color = Texto.copy(alpha = 0.85f), fontSize = 12.5.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            if (!estado.temBluetooth) {
                Text(
                    "Ver fones conectados", color = MaterialTheme.colorScheme.primary, fontSize = 12.5.sp,
                    style = TextStyle(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = acoes.pedirBluetooth),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- controles rapidos

@Composable
private fun Controles(acoes: AcoesPainel, aoTimer: () -> Unit) {
    val contexto = LocalContext.current
    var lanterna by remember { mutableStateOf(Lanterna.ligada) }
    var calculando by remember { mutableStateOf(false) }
    Titulo("Controles rápidos")
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Controle(if (lanterna) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff, "Lanterna", ligado = lanterna) {
            Lanterna.alternar(contexto)
            lanterna = Lanterna.ligada
        }
        Controle(Icons.Outlined.Timer, "Timer", aoTocar = aoTimer)
        Controle(Icons.Outlined.QrCodeScanner, "Scanner", aoTocar = acoes.scanner)
        Controle(Icons.Outlined.Calculate, "Calcular", ligado = calculando) { calculando = !calculando }
    }
    AnimatedVisibility(calculando, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { Calculadora() }
}

@Composable
private fun Controle(icone: ImageVector, nome: String, ligado: Boolean = false, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = aoTocar).padding(vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (ligado) Brush.linearGradient(listOf(cor, MaterialTheme.colorScheme.secondary)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                .border(1.dp, if (ligado) Color.Transparent else Texto.copy(alpha = 0.22f), CircleShape),
        ) { Icon(icone, contentDescription = nome, tint = if (ligado) Fundo else Texto, modifier = Modifier.size(23.dp)) }
        Text(nome, color = Texto.copy(alpha = 0.85f), fontSize = 12.5.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

/** A calculadora: escreve a conta e o resultado aparece na hora (mesma conta da busca). */
@Composable
private fun Calculadora() {
    val cor = MaterialTheme.colorScheme.primary
    var conta by remember { mutableStateOf("") }
    val resultado = remember(conta) { interpretar(conta).firstOrNull { it.simbolo == "=" }?.titulo }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
        BasicTextField(
            value = conta,
            onValueChange = { conta = it },
            singleLine = true,
            textStyle = TextStyle(color = Texto, fontSize = 20.sp, fontWeight = FontWeight.Light),
            cursorBrush = SolidColor(cor),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
            decorationBox = { campo ->
                Box {
                    if (conta.isEmpty()) Text("150 * 0,15", color = TextoFraco, fontSize = 20.sp, fontWeight = FontWeight.Light)
                    campo()
                }
            },
            modifier = Modifier.weight(1f).focusRequester(foco),
        )
        Text(resultado?.let { "= $it" } ?: "", color = cor, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
    Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(cor, Color.Transparent))))
}

// ---------------------------------------------------------------- timer

@Composable
private fun TimerCorrendo(estado: EstadoPainel, acoes: AcoesPainel) {
    val t = estado.timer
    val agora = aCada(1000, estado.timerMudou) { System.currentTimeMillis() }
    Titulo("Timer")
    if (t.fim > 0) {
        val falta = (t.fim - agora).coerceAtLeast(0)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
            Column(Modifier.weight(1f)) {
                Text(t.rotulo, color = TextoFraco, fontSize = 13.sp)
                Text(relogio(falta), color = Texto, fontSize = 34.sp, fontWeight = FontWeight.Light)
            }
            Text(
                "Parar", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = acoes.pararTimer).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Barra(if (t.duracao > 0) 1f - falta.toFloat() / t.duracao else 0f, Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
    }
    if (t.cronoInicio > 0 || t.cronoAcumulado > 0) {
        val passou = t.cronoAcumulado + if (t.cronoInicio > 0) agora - t.cronoInicio else 0
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Cronômetro", color = TextoFraco, fontSize = 13.sp)
                Text(relogio(passou), color = Texto, fontSize = 34.sp, fontWeight = FontWeight.Light)
            }
            Text(
                if (t.cronoInicio > 0) "Pausar" else "Continuar", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = acoes.alternarCrono).padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Text(
                "Zerar", color = TextoFraco, fontSize = 14.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = acoes.zerarCrono).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun relogio(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}

/** Escolher o timer: os tempos comuns, o Pomodoro e o cronometro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EscolherTimer(t: TimerWolf, acoes: AcoesPainel, aoFechar: () -> Unit) {
    Folha(aoFechar) {
        Text("Timer", color = Texto, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        SecaoMenu("Contagem regressiva")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            for (m in listOf(1, 3, 5, 10, 15, 20, 30, 45, 60)) {
                Pilula(if (m == 60) "1 h" else "$m min", destaque = false) { acoes.comecarTimer(m, "Timer de $m min"); aoFechar() }
            }
        }
        SecaoMenu("Foco")
        LinhaMenu(Icons.Outlined.Timer, "Pomodoro", "25 min de foco; depois, uma pausa de 5") { acoes.comecarTimer(25, "Pomodoro"); aoFechar() }
        LinhaMenu(Icons.Outlined.Timer, "Pausa curta", "5 min") { acoes.comecarTimer(5, "Pausa"); aoFechar() }
        SecaoMenu("Cronômetro")
        LinhaMenu(
            Icons.Outlined.Timer, if (t.cronoInicio > 0) "Pausar o cronômetro" else "Começar o cronômetro",
            "conta no próprio Painel",
        ) { acoes.alternarCrono(); aoFechar() }
    }
}

// ---------------------------------------------------------------- notas e tarefas

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotasRapidas(notas: List<Nota>, acoes: AcoesPainel) {
    val cor = MaterialTheme.colorScheme.primary
    var escrevendo by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var tarefa by remember { mutableStateOf(true) }
    val feitas = notas.count { it.tarefa && it.feita }
    val tarefas = notas.count { it.tarefa }
    Titulo(if (tarefas > 0) "Notas e tarefas · $feitas/$tarefas" else "Notas rápidas") {
        if (feitas > 0) {
            Text(
                "Limpar feitas", color = TextoFraco, fontSize = 12.5.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = acoes.limparFeitas).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        BotaoIcone(if (escrevendo) Icons.Outlined.Close else Icons.Outlined.Add, "Nova nota") { escrevendo = !escrevendo; texto = "" }
    }
    AnimatedVisibility(escrevendo, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val foco = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
        Column(Modifier.padding(horizontal = 24.dp)) {
            BasicTextField(
                value = texto,
                onValueChange = { texto = it },
                singleLine = true,
                textStyle = TextStyle(color = Texto, fontSize = 16.sp),
                cursorBrush = SolidColor(cor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (texto.isNotBlank()) { acoes.adicionarNota(texto, tarefa); texto = "" }
                }),
                decorationBox = { campo ->
                    Box {
                        if (texto.isEmpty()) Text(if (tarefa) "Nova tarefa…" else "Nova nota…", color = TextoFraco, fontSize = 16.sp)
                        campo()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(foco),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(cor, Color.Transparent))))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Pilula("Tarefa", destaque = tarefa) { tarefa = true }
                Pilula("Nota", destaque = !tarefa) { tarefa = false }
            }
        }
    }
    if (notas.isEmpty() && !escrevendo) {
        Text(
            "Nada anotado. Toque no + ou escreva na busca: “nota …” ou “tarefa …”.",
            color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
    // as tarefas por fazer primeiro, depois as notas, e as feitas no fim
    for (n in notas.sortedWith(compareBy<Nota> { if (it.tarefa && it.feita) 2 else if (it.tarefa) 0 else 1 }.thenByDescending { it.criada })) {
        key(n.id) {
            var apagar by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { if (n.tarefa) acoes.alternarNota(n.id) }, onLongClick = { apagar = !apagar })
                    .padding(horizontal = 20.dp, vertical = 7.dp),
            ) {
                if (n.tarefa) {
                    Icon(
                        if (n.feita) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null,
                        tint = if (n.feita) cor else TextoFraco, modifier = Modifier.size(22.dp),
                    )
                } else {
                    Box(Modifier.padding(horizontal = 8.dp).size(6.dp).clip(CircleShape).background(cor))
                }
                Text(
                    n.texto,
                    color = if (n.feita) TextoFraco else Texto,
                    fontSize = 15.5.sp,
                    style = TextStyle(textDecoration = if (n.feita) TextDecoration.LineThrough else null),
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                if (apagar) BotaoIcone(Icons.Outlined.Close, "Apagar", Perigo) { acoes.apagarNota(n.id) }
            }
        }
    }
}

// ---------------------------------------------------------------- compromissos e espaco

@Composable
private fun Proximos(estado: EstadoPainel, acoes: AcoesPainel) {
    Titulo("Próximos compromissos")
    when {
        !estado.temAgenda -> Text(
            "Permitir ver a agenda", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
            style = TextStyle(textDecoration = TextDecoration.Underline),
            modifier = Modifier.padding(horizontal = 24.dp).clickable(onClick = acoes.permitirAgenda),
        )
        estado.eventos.isEmpty() -> Text("Nada nas próximas 24 h.", color = TextoFraco, fontSize = 13.5.sp, modifier = Modifier.padding(horizontal = 24.dp))
        else -> for (e in estado.eventos.take(3)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { acoes.abrirEvento(e) }.padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(
                    if (e.diaInteiro) "dia" else LocalDateTime.ofInstant(Instant.ofEpochMilli(e.inicio), ZoneId.systemDefault()).format(HM),
                    color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(52.dp),
                )
                Text(e.titulo, color = Texto, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (e.link != null) {
                    Text(
                        "Entrar", color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).clickable { acoes.entrarEvento(e) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Espaco() {
    val (livre, total) = aCada(60_000) { Armazenamento.agora() }
    if (total <= 0) return
    Titulo("Armazenamento")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
        Icon(Icons.Outlined.SdStorage, contentDescription = null, tint = TextoFraco, modifier = Modifier.size(18.dp))
        Text(
            "${Armazenamento.texto(livre)} livres de ${Armazenamento.texto(total)}", color = Texto, fontSize = 15.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
    Barra(1f - livre.toFloat() / total, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

// ---------------------------------------------------------------- widgets do Android

@Composable
private fun WidgetsDoAndroid(widgets: List<WidgetSalvo>, acoes: AcoesWidgets) {
    var escolhendo by remember { mutableStateOf(false) }
    Titulo("Widgets") { BotaoIcone(Icons.Outlined.Add, "Adicionar widget") { escolhendo = true } }
    if (widgets.isEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(12.dp)).clickable { escolhendo = true }.padding(vertical = 6.dp),
        ) {
            Icon(Icons.Outlined.Widgets, contentDescription = null, tint = TextoFraco, modifier = Modifier.size(18.dp))
            Text("Adicionar um widget do Android", color = TextoFraco, fontSize = 13.5.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
    for (w in widgets) key(w.id) { WidgetAndroid(w, acoes) }
    if (escolhendo) EscolherWidget(aoEscolher = { info -> escolhendo = false; acoes.adicionar(info) }, aoFechar = { escolhendo = false })
}

// ---------------------------------------------------------------- personalizar

@Composable
private fun Personalizar(estado: EstadoPainel, acoes: AcoesPainel, aoFechar: () -> Unit) {
    Folha(aoFechar) {
        Text("Personalizar painel", color = Texto, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Text(
            "A chave mostra ou esconde cada parte; as setas mudam a ordem. Timer e música só aparecem quando estão em uso.",
            color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        )
        estado.ordem.forEachIndexed { i, p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)) {
                Text(p.titulo, color = if (p in estado.escondidas) TextoFraco else Texto, fontSize = 16.sp, modifier = Modifier.weight(1f))
                BotaoIcone(Icons.Outlined.KeyboardArrowUp, "Subir", if (i > 0) Texto else TextoFraco.copy(alpha = 0.3f)) { acoes.moverParte(p, -1) }
                BotaoIcone(Icons.Outlined.KeyboardArrowDown, "Descer", if (i < estado.ordem.lastIndex) Texto else TextoFraco.copy(alpha = 0.3f)) { acoes.moverParte(p, 1) }
                Spacer(Modifier.width(6.dp))
                ChaveWolf(p !in estado.escondidas) { acoes.alternarParte(p) }
            }
        }
    }
}
