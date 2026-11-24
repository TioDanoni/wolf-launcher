package org.wolfos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
    avisosPermitidos: Boolean,
    aoPermitirAvisos: () -> Unit,
    aoInformacoesDoLauncher: () -> Unit,
    mostrarMatilha: Boolean,
    aoMostrarMatilha: (Boolean) -> Unit,
    ritmo: Boolean,
    aoRitmo: (Boolean) -> Unit,
    modoNoite: Boolean,
    aoModoNoite: (Boolean) -> Unit,
    janelaNoite: Pair<Int, Int>,
    aoJanelaNoite: (Int, Int) -> Unit,
    aoFechar: () -> Unit,
) {
    var procurando by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Cabecalho("Ajustes", aoFechar)

        Grupo("Tela inicial")
        Chave("Matilha", "as pessoas no polegar (toque no + para escolher)", mostrarMatilha, aoMostrarMatilha)
        if (avisosPermitidos) {
            Linha("Prévias das notificações", "ligadas: os verbos e a Matilha mostram quem está esperando", aoTocar = aoPermitirAvisos)
        } else {
            Linha("Prévias das notificações", "tocar para permitir o acesso às notificações", aoTocar = aoPermitirAvisos)
            Linha(
                "O Android disse “configuração restrita”?",
                "Toque aqui, depois em ⋮ (canto de cima) › Permitir configurações restritas, e volte para permitir",
                aoTocar = aoInformacoesDoLauncher,
            )
        }

        Grupo("Ritmo do dia")
        Chave("Verbos mudam com a hora", "trabalho de tarde, música com o fone, e o que você mais abre em cada hora", ritmo, aoRitmo)
        Chave("Modo noite", "cor calma e só os verbos da noite, com o despertador no topo", modoNoite, aoModoNoite)
        if (modoNoite) JanelasNoite(janelaNoite, aoJanelaNoite)

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

private val JANELAS = listOf(22 * 60 to 6 * 60, 22 * 60 + 30 to 7 * 60, 23 * 60 to 7 * 60 + 30, 0 to 8 * 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JanelasNoite(atual: Pair<Int, Int>, aoEscolher: (Int, Int) -> Unit) {
    fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        for ((inicio, fim) in JANELAS) {
            Pilula("${hhmm(inicio)} – ${hhmm(fim)}", destaque = atual == inicio to fim) { aoEscolher(inicio, fim) }
        }
    }
}
