package org.wolfos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Vermelho das acoes que tiram algo (desinstalar). */
val Perigo = Color(0xFFFF6B6B)

/** A folha que sobe de baixo, usada em todos os menus do launcher. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folha(aoFechar: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Superficie,
        contentColor = Texto,
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            content = conteudo,
        )
    }
}

/** Titulo pequeno de uma parte do menu. */
@Composable
fun SecaoMenu(titulo: String, modifier: Modifier = Modifier) {
    Rotulo(titulo, modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp))
}

/**
 * Uma linha de menu: o desenho, o titulo, uma explicacao curta e (talvez) algo na
 * ponta (chave, valor, seta).
 */
@Composable
fun LinhaMenu(
    icone: ImageVector,
    titulo: String,
    sub: String? = null,
    cor: Color = Texto,
    fim: (@Composable () -> Unit)? = null,
    aoTocar: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(horizontal = 24.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cor.copy(alpha = 0.10f)),
        ) { Icon(icone, contentDescription = null, tint = if (cor == Texto) MaterialTheme.colorScheme.primary else cor, modifier = Modifier.size(21.dp)) }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
            Text(titulo, color = cor, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, color = TextoFraco, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        fim?.invoke()
    }
}

/** A seta de "abre outra tela". */
@Composable
fun Seta() {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextoFraco)
}

/** Texto curto na ponta da linha (o valor atual). */
@Composable
fun Valor(texto: String) {
    Text(texto, color = TextoFraco, fontSize = 13.5.sp, maxLines = 1)
}

/** A chave liga/desliga na cor de destaque. */
@Composable
fun ChaveWolf(ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Switch(
        checked = ligado,
        onCheckedChange = aoMudar,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = Fundo,
            uncheckedTrackColor = Texto.copy(alpha = 0.1f),
            uncheckedBorderColor = Texto.copy(alpha = 0.25f),
            uncheckedThumbColor = Texto.copy(alpha = 0.6f),
        ),
    )
}

/** Linha fina entre partes. */
@Composable
fun Divisoria(modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(Texto.copy(alpha = 0.08f)))
}
