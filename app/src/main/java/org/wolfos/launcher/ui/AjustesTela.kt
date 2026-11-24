package org.wolfos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.atualizacao.Atualizador
import org.wolfos.launcher.atualizacao.EstadoAtualizacao

/** Os ajustes do launcher. Por enquanto: atualizacoes e papel de parede. */
@Composable
fun AjustesTela(
    atualizacao: EstadoAtualizacao,
    instalarSozinho: Boolean,
    aoInstalarSozinho: (Boolean) -> Unit,
    aoAtualizar: () -> Unit,
    aoProcurar: ((Boolean) -> Unit) -> Unit,
    aoPapelDeParede: () -> Unit,
    aoFechar: () -> Unit,
) {
    var procurando by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Cabecalho("Ajustes", aoFechar)

        Grupo("Atualizações")
        textoAtualizacao(atualizacao)?.let { (simbolo, texto, botao) ->
            Faixa(simbolo, texto, botao, aoAtualizar, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Linha(
            "Versão instalada",
            Atualizador.versaoAtual,
        )
        Linha("Procurar atualização agora", procurando ?: "tocar para ver") {
            procurando = "procurando…"
            aoProcurar { achou -> procurando = if (achou) "tem versão nova" else "você já está na última" }
        }
        Chave(
            "Instalar sozinho",
            "baixa e instala a versão nova sem precisar de você (com internet e bateria boa)",
            instalarSozinho,
            aoInstalarSozinho,
        )

        Grupo("Visual")
        Linha("Papel de parede", "a cor de destaque vem dele (Android 12+)", aoTocar = aoPapelDeParede)
    }
}

@Composable
private fun Grupo(titulo: String) {
    Rotulo(titulo, Modifier.padding(start = 24.dp, top = 24.dp, bottom = 6.dp), MaterialTheme.colorScheme.primary)
}

@Composable
private fun Linha(titulo: String, sub: String, aoTocar: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(titulo, color = Texto, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(sub, color = TextoFraco, fontSize = 13.sp)
    }
}

@Composable
private fun Chave(titulo: String, sub: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { aoMudar(!ligado) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(titulo, color = Texto, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = TextoFraco, fontSize = 13.sp)
        }
        Switch(
            checked = ligado,
            onCheckedChange = aoMudar,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Fundo,
            ),
        )
    }
}
