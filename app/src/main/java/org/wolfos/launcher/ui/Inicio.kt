package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    /** Abre a barra de comando, ja com um texto (ou vazia). */
    val abrirComando: (String) -> Unit,
    val abrirGaveta: () -> Unit,
    val abrirNotificacoes: () -> Unit,
    val abrirAjustes: () -> Unit,
    val papelDeParede: () -> Unit,
    val atualizar: () -> Unit,
    val definirPadrao: () -> Unit,
    val abrirApp: (AppInfo) -> Unit,
)

/** O que esta esperando num verbo: a ultima notificacao e quantas tem. */
data class Previa(val texto: String, val total: Int)

/** O leque aberto: as letras desenhadas ate agora e onde o dedo parou. */
private data class EstadoLeque(val prefixo: String, val alternativas: List<Char>, val centro: Offset)

/**
 * A tela inicial: relogio, a faixa do agora, os verbos (o que voce quer fazer) e a
 * barra de comando na altura do polegar. Desenhar uma letra em qualquer lugar abre o
 * leque de apps; para cima abre todos os apps; para baixo, as notificacoes; segurar
 * num espaco vazio abre o menu do launcher.
 */
@Composable
fun Inicio(
    apps: List<AppInfo>,
    verbos: Map<Verbo, List<AppInfo>>,
    atualizacao: EstadoAtualizacao,
    ehPadrao: Boolean,
    previas: Map<Verbo, Previa>,
    acoes: AcoesInicio,
    /** A fileira da Matilha (ou nada), logo acima da barra de comando. */
    matilha: @Composable () -> Unit,
) {
    var menuEm by remember { mutableStateOf<Offset?>(null) }
    var leque by remember { mutableStateOf<EstadoLeque?>(null) }

    Box(Modifier.fillMaxSize()) {
        AreaDeDesenho(
            aoDeslizar = { dx, dy ->
                when (direcao(dx, dy)) {
                    -1 -> acoes.abrirGaveta()
                    1 -> acoes.abrirNotificacoes()
                }
            },
            aoLetra = { letras, fim -> leque = EstadoLeque(letras.first().toString(), letras.drop(1), fim) },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { menuEm = it }) },
        ) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
                Relogio(Modifier.padding(start = 10.dp, top = 28.dp))
                // a faixa do agora: primeiro o que falta para o launcher funcionar direito
                if (!ehPadrao) {
                    Spacer(Modifier.height(16.dp))
                    Faixa("⌂", "Usar o WolfLauncher como tela inicial", "Definir", acoes.definirPadrao)
                }
                textoAtualizacao(atualizacao)?.let { (simbolo, texto, botao) ->
                    Spacer(Modifier.height(if (ehPadrao) 16.dp else 8.dp))
                    Faixa(simbolo, texto, botao, acoes.atualizar)
                }
                // os verbos ficam embaixo, perto do polegar; se nao couberem, rolam
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.align(Alignment.BottomStart).verticalScroll(rememberScrollState()),
                    ) {
                        for (v in verbos.keys) LinhaVerbo(v, previas[v]) { acoes.abrirVerbo(v) }
                        if (verbos.isEmpty()) {
                            Text("Lendo seus apps…", color = TextoFraco, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                matilha()
                Spacer(Modifier.height(12.dp))
                BarraComandoFechada({ acoes.abrirComando("") })
                Text(
                    "desenhe uma letra  ·  ︿ todos os apps  ·  segure para ajustes",
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

        leque?.let { l ->
            Leque(
                apps = apps,
                prefixo = l.prefixo,
                alternativas = l.alternativas,
                centro = l.centro,
                aoAbrir = { app -> leque = null; acoes.abrirApp(app) },
                aoTrocarLetra = { c ->
                    // a letra reconhecida estava errada: troca a ultima e guarda a errada como alternativa
                    val errada = l.prefixo.last()
                    leque = l.copy(prefixo = l.prefixo.dropLast(1) + c, alternativas = l.alternativas - c + errada)
                },
                aoMaisLetras = { letras -> leque = l.copy(prefixo = l.prefixo + letras.first(), alternativas = letras.drop(1)) },
                aoVerTodos = { leque = null; acoes.abrirComando(l.prefixo.lowercase()) },
                aoFechar = { leque = null },
            )
        }
    }
}

/** Um verbo na tela inicial; com algo esperando, fica mais forte e mostra a previa. */
@Composable
private fun LinhaVerbo(verbo: Verbo, previa: Previa?, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                verbo.titulo,
                fontSize = 34.sp,
                fontWeight = if (previa != null) FontWeight.Medium else FontWeight.Light,
                color = Texto,
            )
            if (previa != null) {
                Box(Modifier.padding(start = 10.dp, bottom = 14.dp).size(8.dp).clip(CircleShape).background(cor))
            }
        }
        if (previa != null) {
            Text(
                previa.texto + if (previa.total > 1) "  ·  +${previa.total - 1}" else "",
                color = cor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp),
            )
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
