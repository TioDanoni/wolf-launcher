package org.wolfos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.Acao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Conversa
import org.wolfos.launcher.dados.EXEMPLOS
import org.wolfos.launcher.dados.filtrar
import org.wolfos.launcher.dados.interpretar
import org.wolfos.launcher.dados.semAcento

/** O que o buscador faz com cada tipo de resultado. */
class AcoesBusca(
    val abrirConversa: (Conversa) -> Unit,
    val ligar: (Contato) -> Unit,
    val mensagem: (Contato) -> Unit,
    val executar: (Acao) -> Unit,
    val web: (String) -> Unit,
    val ajustes: () -> Unit,
    val todosApps: () -> Unit,
)

/** Um resultado do buscador (para o Enter saber qual e o primeiro). */
private sealed interface Achado {
    data class App(val app: AppInfo) : Achado
    data class Papo(val conversa: Conversa) : Achado
    data class Pessoa(val contato: Contato) : Achado
    data class Feito(val acao: Acao) : Achado
    data object Ajustes : Achado
    data class Web(val termo: String) : Achado
}

/**
 * O buscador universal (deslizar para cima): ja abre com o teclado. Vazio, mostra os
 * recentes, as conversas e atalhos; escrevendo, acha apps, conversas do WhatsApp,
 * pessoas da agenda, contas, timer, alarme, lanterna, ajustes do Android e a web.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuscaTela(
    texto: String,
    aoMudar: (String) -> Unit,
    apps: List<AppInfo>,
    recentes: List<AppInfo>,
    conversas: List<Conversa>,
    contatos: List<Contato>,
    acoesApp: AcoesApp,
    acoes: AcoesBusca,
) {
    val termo = texto.trim()
    val t = semAcento(termo)
    val achados = remember(termo, apps, conversas, contatos) {
        if (termo.isEmpty()) emptyMap()
        else buildMap<String, List<Achado>> {
            filtrar(apps, termo).take(6).map { Achado.App(it) }.takeIf { it.isNotEmpty() }?.let { put("Apps", it) }
            conversas.filter { c -> c.busca.contains(t) }.sortedBy { if (it.busca.startsWith(t)) 0 else 1 }.take(4)
                .map { Achado.Papo(it) }.takeIf { it.isNotEmpty() }?.let { put("Conversas", it) }
            Contatos.filtrar(contatos, termo).take(3).map { Achado.Pessoa(it) }.takeIf { it.isNotEmpty() }?.let { put("Pessoas", it) }
            val feitos = interpretar(termo).map { Achado.Feito(it) } +
                (if ("ajustes do wolflauncher".contains(t) || "wolflauncher".startsWith(t)) listOf(Achado.Ajustes) else emptyList())
            feitos.takeIf { it.isNotEmpty() }?.let { put("Ações e ajustes", it) }
            put("Web", listOf(Achado.Web(termo)))
        }
    }
    val fazer: (Achado) -> Unit = { a ->
        when (a) {
            is Achado.App -> acoesApp.abrir(a.app)
            is Achado.Papo -> acoes.abrirConversa(a.conversa)
            is Achado.Pessoa -> acoes.ligar(a.contato)
            is Achado.Feito -> acoes.executar(a.acao)
            Achado.Ajustes -> acoes.ajustes()
            is Achado.Web -> acoes.web(a.termo)
        }
    }
    val primeiro = achados.values.firstOrNull()?.firstOrNull()

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        CampoBuscaLinha(texto, aoMudar, dica = "Buscar apps, conversas, pessoas, contas…") { primeiro?.let(fazer) }
        LazyColumn(Modifier.fillMaxSize()) {
            if (termo.isEmpty()) {
                if (recentes.isNotEmpty()) {
                    item(key = "t-rec") { Titulo("Recentes") }
                    item(key = "rec") {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            for (app in recentes.take(5)) Atalho(app.nome, { IconeApp(app, 44.dp) }) { acoesApp.abrir(app) }
                        }
                    }
                }
                if (conversas.isNotEmpty()) {
                    item(key = "t-conv") { Titulo("Conversas") }
                    item(key = "conv") {
                        LazyRow(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
                            items(conversas.take(10), key = { it.chave }) { c ->
                                Atalho(Matilha_nome(c), { FotoConversa(c, 44.dp) }) { acoes.abrirConversa(c) }
                            }
                        }
                    }
                }
                item(key = "t-ex") { Titulo("Também dá para escrever") }
                item(key = "ex") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        (EXEMPLOS + "ligar mãe").forEach { e -> Pilula(e, destaque = false) { aoMudar(e) } }
                    }
                }
                item(key = "todos") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp).clickable(onClick = acoes.todosApps).padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Icon(Icons.Outlined.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text("Todos os apps", color = Texto, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
                        Text("${apps.size}", color = TextoFraco, fontSize = 13.sp)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextoFraco)
                    }
                }
            } else {
                achados.forEach { (secao, lista) ->
                    item(key = "t-$secao") { Titulo(secao, Modifier.animateItem()) }
                    lista.forEach { a ->
                        item(key = chave(a)) {
                            Box(Modifier.animateItem()) { LinhaAchado(a, a == primeiro, acoesApp, acoes) { fazer(a) } }
                        }
                    }
                }
            }
        }
    }
}

private fun chave(a: Achado): String = when (a) {
    is Achado.App -> "a:" + a.app.chave
    is Achado.Papo -> "c:" + a.conversa.chave
    is Achado.Pessoa -> "p:" + a.contato.id
    is Achado.Feito -> "f:" + a.acao.titulo
    Achado.Ajustes -> "ajustes"
    is Achado.Web -> "web"
}

private fun Matilha_nome(c: Conversa) = org.wolfos.launcher.dados.Conversas.nomeCurto(c)

@Composable
private fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)) {
        Text(texto, color = Texto.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.padding(start = 8.dp).size(width = 16.dp, height = 2.dp).clip(CircleShape).background(degradeH()))
    }
}

/** Um icone com o nome embaixo (recentes, conversas). */
@Composable
private fun Atalho(nome: String, icone: @Composable () -> Unit, aoTocar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(66.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = aoTocar).padding(vertical = 6.dp),
    ) {
        icone()
        Text(nome, color = Texto.copy(alpha = 0.85f), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp))
    }
}

/** Uma linha de resultado; a primeira (a do Enter) ganha um traco na cor da paleta a esquerda. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaAchado(a: Achado, primeiro: Boolean, acoesApp: AcoesApp, acoes: AcoesBusca, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = aoTocar, onLongClick = { if (a is Achado.App) menu = true })
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(Modifier.width(3.dp).height(30.dp).clip(CircleShape).background(if (primeiro) degrade() else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))))
            Box(Modifier.padding(start = 9.dp).size(40.dp), contentAlignment = Alignment.Center) {
                when (a) {
                    is Achado.App -> IconeApp(a.app, 38.dp)
                    is Achado.Papo -> FotoConversa(a.conversa, 38.dp)
                    is Achado.Pessoa -> Avatar(a.contato.nome, 38.dp, a.contato.foto)
                    is Achado.Feito -> Text(a.acao.simbolo, color = cor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Achado.Ajustes -> Text("⚙", color = cor, fontSize = 20.sp)
                    is Achado.Web -> Icon(Icons.Outlined.Search, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
                }
            }
            val (titulo, sub) = when (a) {
                is Achado.App -> a.app.nome to null
                is Achado.Papo -> a.conversa.nome to (if (a.conversa.grupo) "grupo no WhatsApp" else "conversa no WhatsApp")
                is Achado.Pessoa -> a.contato.nome to a.contato.numero
                is Achado.Feito -> a.acao.titulo to a.acao.sub
                Achado.Ajustes -> "Ajustes do WolfLauncher" to null
                is Achado.Web -> "Buscar “${a.termo}” na web" to null
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(titulo, color = Texto, fontSize = 16.5.sp, fontWeight = if (primeiro) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!sub.isNullOrBlank()) Text(sub, color = TextoFraco, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (a is Achado.Pessoa) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(42.dp).clip(CircleShape).clickable { acoes.mensagem(a.contato) },
                ) { Icon(Icons.AutoMirrored.Outlined.Message, contentDescription = "Mensagem", tint = cor, modifier = Modifier.size(20.dp)) }
            }
        }
        if (a is Achado.App) MenuApp(a.app, menu, { menu = false }, acoesApp)
    }
}

/**
 * O campo de busca em linha fina (sem caixa): a lupa na cor da paleta, o texto grande
 * e um traco em degrade embaixo, mais forte com o teclado aberto. Ja abre com o
 * teclado quando [autoFoco].
 */
@Composable
fun CampoBuscaLinha(
    texto: String,
    aoMudar: (String) -> Unit,
    dica: String = "Buscar apps",
    autoFoco: Boolean = true,
    aoEnviar: () -> Unit,
) {
    val cor = MaterialTheme.colorScheme.primary
    val foco = remember { FocusRequester() }
    var focado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (autoFoco) runCatching { foco.requestFocus() } }
    // o cursor vai para o fim quando o texto vem de fora (os atalhos de exemplo)
    var valor by remember { mutableStateOf(TextFieldValue(texto, TextRange(texto.length))) }
    if (valor.text != texto) valor = TextFieldValue(texto, TextRange(texto.length))
    val linha by animateFloatAsState(if (focado) 1f else 0.35f, label = "linha")
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
            BasicTextField(
                value = valor,
                onValueChange = {
                    valor = it
                    if (it.text != texto) aoMudar(it.text)
                },
                singleLine = true,
                textStyle = TextStyle(color = Texto, fontSize = 19.sp, fontWeight = FontWeight.Light),
                cursorBrush = SolidColor(cor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { aoEnviar() }),
                decorationBox = { campo ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (texto.isEmpty()) Text(dica, color = TextoFraco, fontSize = 19.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        campo()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .focusRequester(foco)
                    .onFocusChanged { focado = it.isFocused },
            )
            if (texto.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { aoMudar("") },
                ) { Icon(Icons.Outlined.Close, contentDescription = "Limpar", tint = TextoFraco, modifier = Modifier.size(18.dp)) }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .graphicsLayer { alpha = linha }
                .background(Brush.horizontalGradient(listOf(cor, MaterialTheme.colorScheme.secondary, Color.Transparent))),
        )
    }
}
