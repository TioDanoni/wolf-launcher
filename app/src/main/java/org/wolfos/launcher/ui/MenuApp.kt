package org.wolfos.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.AtalhoApp
import org.wolfos.launcher.dados.Atalhos
import org.wolfos.launcher.dados.Verbo

/** O que da para fazer com um app (a tela principal liga cada acao). */
class AcoesApp(
    val abrir: (AppInfo) -> Unit,
    val verboDe: (AppInfo) -> Verbo?,
    val mudarVerbo: (AppInfo, Verbo?) -> Unit,
    val informacoes: (AppInfo) -> Unit,
    val desinstalar: (AppInfo) -> Unit,
    /** Nulo volta ao nome original. */
    val renomear: (AppInfo, String?) -> Unit = { _, _ -> },
    val esconder: (AppInfo) -> Unit = {},
    val abrirAtalho: (AtalhoApp) -> Unit = {},
    /** O nome de cada verbo (a pessoa pode ter trocado). */
    val tituloDoVerbo: (Verbo) -> String = { it.titulo },
)

/**
 * O menu de segurar um app, numa folha que sobe de baixo: os atalhos do proprio app
 * (nova conversa, tirar foto...) e o que o launcher faz com ele: mudar de verbo,
 * renomear, esconder, informacoes e desinstalar.
 */
@Composable
fun MenuApp(app: AppInfo, aberto: Boolean, aoFechar: () -> Unit, acoes: AcoesApp) {
    if (!aberto) return
    val contexto = LocalContext.current
    val atalhos by produceState(emptyList<AtalhoApp>(), app.chave) {
        value = withContext(Dispatchers.IO) { Atalhos.doApp(contexto, app) }
    }
    var modo by remember { mutableStateOf<String?>(null) }
    val atual = acoes.verboDe(app)

    Folha(aoFechar) {
        // quem e o app
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            IconeApp(app, 52.dp)
            Column(Modifier.padding(start = 16.dp)) {
                Text(app.nome, color = Texto, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        atual?.let { "em ${acoes.tituloDoVerbo(it)}" } ?: "fora dos verbos",
                        app.nomeOriginal.takeIf { it != app.nome }?.let { "nome original: $it" },
                    ).joinToString(" · "),
                    color = TextoFraco,
                    fontSize = 13.sp,
                )
            }
        }

        if (atalhos.isNotEmpty()) {
            SecaoMenu("Atalhos")
            for (a in atalhos) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aoFechar(); acoes.abrirAtalho(a) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    a.icone?.let { Image(it, contentDescription = null, modifier = Modifier.size(34.dp).clip(CircleShape)) }
                        ?: Box(Modifier.size(34.dp).clip(CircleShape).background(Texto.copy(alpha = 0.08f)))
                    Text(a.rotulo, color = Texto, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 16.dp))
                }
            }
            Divisoria(Modifier.padding(top = 8.dp))
        }

        SecaoMenu("No launcher")
        LinhaMenu(
            Icons.Outlined.Category,
            "Mudar de verbo",
            atual?.let { "agora em ${acoes.tituloDoVerbo(it)}" } ?: "agora fora dos verbos",
        ) { modo = if (modo == "verbo") null else "verbo" }
        if (modo == "verbo") EscolherVerbo(atual, acoes.tituloDoVerbo) { v -> acoes.mudarVerbo(app, v); aoFechar() }

        LinhaMenu(Icons.Outlined.DriveFileRenameOutline, "Renomear", "só no launcher; o app continua igual") {
            modo = if (modo == "nome") null else "nome"
        }
        if (modo == "nome") {
            CampoNome(app.nome, app.nomeOriginal.takeIf { it != app.nome }) { novo -> acoes.renomear(app, novo); aoFechar() }
        }

        LinhaMenu(Icons.Outlined.VisibilityOff, "Esconder", "some dos verbos, da busca e da lista; volta em Ajustes") {
            aoFechar(); acoes.esconder(app)
        }
        LinhaMenu(Icons.Outlined.Info, "Informações do app", "permissões, armazenamento, notificações") {
            aoFechar(); acoes.informacoes(app)
        }
        LinhaMenu(Icons.Outlined.DeleteOutline, "Desinstalar", cor = Perigo) { aoFechar(); acoes.desinstalar(app) }
    }
}

/** Os verbos em pilulas para escolher (e "nenhum", que deixa o app so na lista). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EscolherVerbo(atual: Verbo?, titulo: (Verbo) -> String, aoEscolher: (Verbo?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 78.dp, end = 24.dp, bottom = 10.dp),
    ) {
        for (v in Verbo.entries) Pilula(titulo(v), destaque = v == atual) { aoEscolher(v) }
        Pilula("Nenhum", destaque = atual == null) { aoEscolher(null) }
    }
}

/** O campo de trocar o nome, com salvar e (se ja foi trocado) voltar ao original. */
@Composable
fun CampoNome(atual: String, original: String?, aoSalvar: (String?) -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    var texto by remember { mutableStateOf(atual) }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Column(Modifier.padding(start = 78.dp, end = 24.dp, bottom = 10.dp)) {
        BasicTextField(
            value = texto,
            onValueChange = { texto = it },
            singleLine = true,
            textStyle = TextStyle(color = Texto, fontSize = 16.sp),
            cursorBrush = SolidColor(cor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { aoSalvar(texto) }),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Fundo.copy(alpha = 0.6f))
                .border(1.dp, cor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .focusRequester(foco),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            Pilula("Salvar", destaque = true) { aoSalvar(texto) }
            if (original != null) Pilula("Voltar para “$original”", destaque = false) { aoSalvar(null) }
        }
    }
}

/** O desenho pequeno de verbo (para quem quiser usar fora da tela inicial). */
@Composable
fun IconeVerbo(v: Verbo, modifier: Modifier = Modifier) {
    Icon(iconeDoVerbo(v), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = modifier)
}
