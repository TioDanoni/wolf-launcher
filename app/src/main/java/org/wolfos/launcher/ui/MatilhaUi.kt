package org.wolfos.launcher.ui

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
) {
    Column(modifier) {
        Rotulo("Matilha", Modifier.padding(start = 10.dp, bottom = 8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            for (p in pessoas) Membro(p, Matilha.esperando(p, avisos), acoes)
            if (pessoas.size < Matilha.MAXIMO) {
                Adicionar(if (pessoas.isEmpty()) (if (temContatos) "escolher" else "permitir agenda") else null, acoes.adicionar)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Membro(pessoa: Contato, esperando: List<Aviso>, acoes: AcoesPessoa) {
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
                color = if (esperando.isNotEmpty()) Texto else Texto.copy(alpha = 0.8f),
                fontSize = 11.5.sp,
                style = TextStyle(shadow = SombraTexto),
                fontWeight = if (esperando.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val item = @Composable { texto: String, acao: () -> Unit ->
                DropdownMenuItem(text = { Text(texto, maxLines = 2, overflow = TextOverflow.Ellipsis) }, onClick = { menu = false; acao() })
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
    return this.border(2.5.dp, cor.copy(alpha = alfa), CircleShape)
}
