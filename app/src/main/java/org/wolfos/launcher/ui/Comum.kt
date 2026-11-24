package org.wolfos.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Icones

/** O icone do app, carregado so quando aparece (ate la, um circulo discreto). */
@Composable
fun IconeApp(app: AppInfo, tamanho: Dp, modifier: Modifier = Modifier, mono: Boolean = false) {
    val contexto = LocalContext.current
    val icone by produceState(if (mono) Icones.monoDaMemoria(app) else Icones.daMemoria(app), app.chave, mono) {
        if (value == null) value = if (mono) Icones.carregarMono(contexto, app) else Icones.carregar(contexto, app)
    }
    val m = modifier.size(tamanho)
    icone?.let { Image(it, contentDescription = null, modifier = m) }
        ?: Box(m.clip(CircleShape).background(Superficie))
}

/** Titulo pequeno de secao, em maiusculas e espacado ("PARA AGORA", "APPS"). */
@Composable
fun Rotulo(texto: String, modifier: Modifier = Modifier, cor: androidx.compose.ui.graphics.Color = TextoFraco) {
    Text(
        texto.uppercase(),
        color = cor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = modifier,
    )
}

/**
 * A faixa do "agora": uma linha com o que importa neste momento e o botao da acao.
 * Hoje mostra a atualizacao; depois, o proximo compromisso.
 */
@Composable
fun Faixa(simbolo: String, texto: String, botao: String?, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cor.copy(alpha = 0.10f))
            .border(1.dp, cor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .clickable(onClick = aoTocar)
            .padding(start = 16.dp, end = 6.dp)
            .height(44.dp),
    ) {
        Text(simbolo, color = cor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            texto,
            color = Texto.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        if (botao != null) {
            Box(
                Modifier.clip(CircleShape).background(cor).padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(botao, color = Fundo, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

/** Uma faixa pronta: simbolo, texto, botao e o que o toque faz. */
class FaixaInfo(val simbolo: String, val texto: String, val botao: String?, val acao: () -> Unit)

/** Texto e botao do aviso de atualizacao, conforme o momento. Nulo: nada a mostrar. */
fun textoAtualizacao(e: EstadoAtualizacao): Triple<String, String, String?>? = when (e) {
    EstadoAtualizacao.EmDia -> null
    is EstadoAtualizacao.Disponivel -> Triple("⬆", "WolfLauncher ${e.versao.nome} disponível", "Atualizar")
    is EstadoAtualizacao.PrecisaPermissao -> Triple("⬆", "Permita instalar a versão ${e.versao.nome}", "Permitir")
    is EstadoAtualizacao.Baixando -> Triple("⬇", "Baixando ${e.versao.nome}… ${(e.progresso * 100).toInt()}%", null)
    is EstadoAtualizacao.Instalando -> Triple("⬆", "Instalando ${e.versao.nome}…", null)
    is EstadoAtualizacao.Confirmar -> Triple("⬆", "Versão ${e.versao.nome} pronta", "Concluir")
    is EstadoAtualizacao.Erro -> Triple("!", "Atualização: ${e.mensagem}", if (e.versao != null) "Tentar" else null)
}

/** A barra de comando, na altura do polegar. Na tela inicial so abre a busca. */
@Composable
fun BarraComandoFechada(aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Superficie.copy(alpha = 0.96f))
            .border(1.dp, cor, CircleShape)
            .clickable(onClick = aoTocar)
            .padding(horizontal = 12.dp)
            .height(54.dp),
    ) {
        MarcaUivo()
        Text("Buscar ou mandar: “ligar mãe”", color = TextoFraco, fontSize = 14.5.sp)
    }
}

/** O circulo de destaque com a marca da barra de comando. */
@Composable
fun MarcaUivo() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
    ) { Text("◗", color = Fundo, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

/** Coluna com o titulo grande de uma tela (ajustes, widgets) e o botao de fechar. */
@Composable
fun Cabecalho(titulo: String, aoFechar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
    ) {
        Text(titulo, fontSize = 40.sp, fontWeight = FontWeight.Medium, color = Texto, modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = aoFechar),
        ) { Text("✕", color = TextoFraco, fontSize = 18.sp) }
    }
}

