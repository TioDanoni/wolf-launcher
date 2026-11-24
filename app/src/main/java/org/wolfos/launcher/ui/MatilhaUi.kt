package org.wolfos.launcher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import org.wolfos.launcher.dados.Conversa
import org.wolfos.launcher.dados.Conversas
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Matilha

/** O que da para fazer com uma pessoa da Matilha. */
class AcoesPessoa(
    val ligar: (Contato) -> Unit,
    val mensagem: (Contato) -> Unit,
    /** Nulo quando o WhatsApp nao esta instalado. */
    val whatsapp: ((Contato) -> Unit)?,
    val verNaAgenda: (Contato) -> Unit,
    val abrirAviso: (Aviso) -> Unit,
    val tirar: (Contato) -> Unit,
    val adicionar: () -> Unit,
)

/**
 * A fileira da Matilha: as pessoas no polegar. O anel na cor de destaque quer dizer
 * que a pessoa esta esperando resposta. Tocar abre o que fazer com ela.
 */
@Composable
fun FileiraMatilha(
    pessoas: List<Contato>,
    avisos: List<Aviso>,
    temContatos: Boolean,
    acoes: AcoesPessoa,
    modifier: Modifier = Modifier,
    /** Os 8 ultimos digitos de quem ligou hoje e nao foi atendido. */
    perdidas: Set<String> = emptySet(),
) {
    Column(modifier) {
        TituloMatilha()
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            pessoas.forEachIndexed { i, p -> Box(Modifier.entrada(i)) { Membro(p, Matilha.esperando(p, avisos), acoes, p.numero.filter(Char::isDigit).takeLast(8).let { it.length == 8 && it in perdidas }) } }
            if (pessoas.size < Matilha.MAXIMO) {
                Adicionar(if (pessoas.isEmpty()) (if (temContatos) "escolher" else "permitir agenda") else null, acoes.adicionar)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Membro(pessoa: Contato, esperando: List<Aviso>, acoes: AcoesPessoa, perdeu: Boolean = false) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(64.dp)
                .clip(CircleShape)
                .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true }),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .then(if (esperando.isNotEmpty()) Modifier.anelPulsando(cor) else Modifier)
                    .padding(5.dp),
            ) { Avatar(pessoa.nome, 46.dp, pessoa.foto) }
            Text(
                Matilha.primeiroNome(pessoa),
                color = if (esperando.isNotEmpty()) cor else Texto.copy(alpha = 0.8f),
                fontSize = 11.5.sp,
                style = TextStyle(shadow = SombraTexto),
                fontWeight = if (esperando.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        val naoLidas = esperando.sumOf { it.mensagens }
        if (naoLidas > 0) SeloMatilha(naoLidas, Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp))
        if (perdeu) SeloLigacao(Modifier.align(Alignment.TopStart).padding(top = 2.dp, start = 4.dp))
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val item = @Composable { texto: String, acao: () -> Unit ->
                DropdownMenuItem(text = { Text(texto, maxLines = 2, overflow = TextOverflow.Ellipsis) }, onClick = { menu = false; acao() })
            }
            if (perdeu) {
                DropdownMenuItem(text = { Text("Ligou e você não atendeu · retornar", color = cor) }, onClick = { menu = false; acoes.ligar(pessoa) })
            }
            esperando.firstOrNull()?.let { a ->
                DropdownMenuItem(
                    text = { Text("“${a.texto}”", color = cor, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { menu = false; acoes.abrirAviso(a) },
                )
            }
            item("Ligar") { acoes.ligar(pessoa) }
            item("Mensagem") { acoes.mensagem(pessoa) }
            acoes.whatsapp?.let { w -> item("WhatsApp") { w(pessoa) } }
            item("Ver na agenda") { acoes.verNaAgenda(pessoa) }
            item("Tirar da matilha") { acoes.tirar(pessoa) }
        }
    }
}

@Composable
private fun Adicionar(legenda: String?, aoTocar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp).clip(CircleShape).clickable(onClick = aoTocar),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(4.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Texto.copy(alpha = 0.06f))
                .border(1.dp, Texto.copy(alpha = 0.12f), CircleShape),
        ) { Text("+", color = Texto.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Light) }
        if (legenda != null) Text(legenda, color = TextoFraco, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
    }
}

/** O anel de quem esta esperando: respira devagar (so anima enquanto alguem espera). */
@Composable
private fun Modifier.anelPulsando(cor: androidx.compose.ui.graphics.Color): Modifier {
    val pulso = rememberInfiniteTransition(label = "anel")
    val alfa by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "alfa",
    )
    val segunda = MaterialTheme.colorScheme.secondary
    return this
        .graphicsLayer { this.alpha = 0.55f + 0.45f * alfa }
        .border(2.5.dp, androidx.compose.ui.graphics.Brush.sweepGradient(listOf(cor, segunda, cor)), CircleShape)
}

// ---------------------------------------------------------------- Matilha pelo WhatsApp

/** O que da para fazer com uma conversa da Matilha. */
class AcoesConversa(
    val abrir: (Conversa) -> Unit,
    val alternarFixada: (Conversa) -> Unit,
    val abrirAviso: (Aviso) -> Unit,
)

/**
 * A Matilha pelas conversas do WhatsApp: as fixadas primeiro, depois quem esta
 * esperando e as recentes. Tocar abre direto na conversa; segurar fixa ou solta.
 */
@Composable
fun FileiraConversas(
    conversas: List<Conversa>,
    todas: List<Conversa>,
    fixadas: List<String>,
    avisos: List<Aviso>,
    acoes: AcoesConversa,
    modifier: Modifier = Modifier,
    perdidas: Set<String> = emptySet(),
    ligar: (String) -> Unit = {},
) {
    var escolhendo by remember { mutableStateOf(false) }
    Column(modifier) {
        TituloMatilha()
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            conversas.forEachIndexed { i, c ->
                val numero = if (c.grupo) "" else c.id.substringBefore('@').filter(Char::isDigit)
                val perdeu = numero.takeLast(8).let { it.length == 8 && it in perdidas }
                Box(Modifier.entrada(i)) {
                    MembroConversa(c, c.chave in fixadas, Conversas.esperando(c, avisos), acoes, perdeu) { ligar("+$numero") }
                }
            }
            Adicionar(if (conversas.isEmpty()) "fixar" else null) { escolhendo = true }
        }
    }
    if (escolhendo) EscolherConversas(todas, fixadas, acoes.alternarFixada) { escolhendo = false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MembroConversa(c: Conversa, fixada: Boolean, esperando: List<Aviso>, acoes: AcoesConversa, perdeu: Boolean = false, retornar: () -> Unit = {}) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(64.dp)
                .clip(CircleShape)
                .combinedClickable(onClick = { acoes.abrir(c) }, onLongClick = { menu = true }),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .then(if (esperando.isNotEmpty()) Modifier.anelPulsando(cor) else Modifier)
                    .padding(5.dp),
            ) {
                FotoConversa(c, 46.dp)
            }
            Text(
                Conversas.nomeCurto(c),
                color = if (esperando.isNotEmpty()) cor else Texto.copy(alpha = 0.8f),
                fontSize = 11.5.sp,
                fontWeight = if (esperando.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = SombraTexto),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        val naoLidas = esperando.sumOf { it.mensagens }
        if (naoLidas > 0) {
            SeloMatilha(naoLidas, Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp))
        }
        if (perdeu) SeloLigacao(Modifier.align(Alignment.TopStart).padding(top = 2.dp, start = 4.dp))
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            Text(
                c.nome + if (c.grupo) "  ·  grupo" else "",
                color = TextoFraco,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (perdeu) {
                DropdownMenuItem(text = { Text("Ligou e você não atendeu · retornar", color = cor) }, onClick = { menu = false; retornar() })
            }
            esperando.firstOrNull()?.let { a ->
                DropdownMenuItem(
                    text = { Text("“${a.texto}”", color = cor, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { menu = false; acoes.abrirAviso(a) },
                )
            }
            DropdownMenuItem(text = { Text("Abrir conversa") }, onClick = { menu = false; acoes.abrir(c) })
            DropdownMenuItem(
                text = { Text(if (fixada) "Soltar da Matilha" else "Fixar na Matilha") },
                onClick = { menu = false; acoes.alternarFixada(c) },
            )
        }
    }
}

/** A foto que o WhatsApp entrega para a conversa (ou a inicial colorida). */
@Composable
fun FotoConversa(c: Conversa, tamanho: androidx.compose.ui.unit.Dp) {
    val contexto = LocalContext.current
    val foto by produceState(Conversas.iconeDaMemoria(c), c.chave) {
        if (value == null) value = Conversas.icone(contexto, c)
    }
    foto?.let {
        Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(tamanho).clip(CircleShape))
    } ?: Avatar(c.nome, tamanho)
}

/** A lista de todas as conversas recentes, para fixar ou soltar. */
@Composable
private fun EscolherConversas(todas: List<Conversa>, fixadas: List<String>, aoAlternar: (Conversa) -> Unit, aoFechar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = aoFechar) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Superficie)
                .padding(vertical = 18.dp),
        ) {
            Text("Fixar na Matilha", color = Texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp))
            Text(
                "As conversas recentes do WhatsApp. As fixadas ficam sempre na fileira, primeiro.",
                color = TextoFraco, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
            if (todas.isEmpty()) {
                Text(
                    "Nenhuma conversa ainda. Converse no WhatsApp e volte aqui.",
                    color = TextoFraco, fontSize = 14.sp, modifier = Modifier.padding(22.dp),
                )
            }
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(todas, key = { it.chave }) { c ->
                    val fixada = c.chave in fixadas
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { aoAlternar(c) }.padding(horizontal = 22.dp, vertical = 8.dp),
                    ) {
                        FotoConversa(c, 40.dp)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(c.nome, color = Texto, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (c.grupo) "grupo" else "pessoa", color = TextoFraco, fontSize = 12.sp)
                        }
                        Text(if (fixada) "Fixada" else "Fixar", color = if (fixada) cor else TextoFraco, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                "Pronto",
                color = cor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).padding(end = 22.dp, top = 8.dp).clip(CircleShape).clickable(onClick = aoFechar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** "Matilha" com um traco na cor de destaque. */
@Composable
private fun TituloMatilha() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)) {
        Icon(Icons.Outlined.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(end = 0.dp))
        Text("Matilha", color = Texto, fontSize = 15.sp, fontWeight = FontWeight.Medium, style = TextStyle(shadow = SombraTexto), modifier = Modifier.padding(start = 8.dp))
        Box(Modifier.padding(start = 10.dp).width(26.dp).height(2.dp).clip(CircleShape).background(degradeH()))
    }
}

/** O numero de mensagens nao lidas em cima da foto (cresce para "99+" sem cortar). */
@Composable
private fun SeloMatilha(n: Int, modifier: Modifier = Modifier) {
    val texto = if (n > 99) "99+" else n.toString()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(19.dp)
            .widthIn(min = 19.dp)
            .clip(CircleShape)
            .background(degrade())
            .border(1.5.dp, Fundo, CircleShape)
            .padding(horizontal = 5.dp),
    ) { Text(texto, color = Fundo, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

/** A ligacao perdida: um telefone pequeno no canto da foto, sem cor de alarme. */
@Composable
private fun SeloLigacao(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(19.dp)
            .clip(CircleShape)
            .background(Texto)
            .border(1.5.dp, Fundo, CircleShape),
    ) {
        androidx.compose.material3.Icon(
            Icons.Rounded.PhoneMissed, null,
            tint = Fundo, modifier = Modifier.size(11.dp),
        )
    }
}
