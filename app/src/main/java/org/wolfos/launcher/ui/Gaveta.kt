package org.wolfos.launcher.ui

import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import org.wolfos.launcher.dados.Acao
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.interpretar
import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.filtrar

/**
 * Todos os apps: a busca no topo, a lista em ordem alfabetica com a letra de cada
 * secao e, na direita, a barra de letras (tocar ou arrastar pula direto).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Gaveta(
    apps: List<AppInfo>,
    acoes: AcoesApp,
    busca: String,
    aoBuscar: (String) -> Unit,
    /** Os abertos por ultimo (aparecem no topo, sem busca). */
    recentes: List<AppInfo> = emptyList(),
    /** Quantas notificacoes cada pacote tem. */
    avisos: Map<String, Int> = emptyMap(),
    /** Fazer uma acao da busca (conta, timer, lanterna, ajuste do Android). */
    aoAcao: (Acao) -> Unit = {},
    /** Buscar o texto na web. */
    aoWeb: (String) -> Unit = {},
    /** As pessoas da agenda (a busca acha "mae" em "Mãe"). */
    contatos: List<Contato> = emptyList(),
    aoLigar: (Contato) -> Unit = {},
    aoMensagem: (Contato) -> Unit = {},
) {
    val estado = rememberLazyListState()
    val escopo = rememberCoroutineScope()
    val lista = filtrar(apps, busca)
    val agrupado = busca.isBlank()
    val secoes = if (agrupado) lista.groupBy { it.letra } else emptyMap()
    val comRecentes = agrupado && recentes.isNotEmpty()
    // indice de cada letra na lista (cabecalho + itens das letras de antes; os recentes vem antes)
    val indices = remember(lista, agrupado, comRecentes) {
        var i = if (comRecentes) 1 else 0
        secoes.mapValues { (_, itens) -> i.also { i += itens.size + 1 } }
    }
    // os apps entram em cascata so logo que a lista abre (nao a cada rolagem)
    val abriu = remember { System.currentTimeMillis() }
    val posicao = remember(lista) { lista.withIndex().associate { (i, a) -> a.chave to i } }
    // a busca tambem entende contas, timer, alarme, lanterna e ajustes do Android
    val acoesBusca = remember(busca) { if (busca.isBlank()) emptyList() else interpretar(busca) }
    val pessoas = remember(busca, contatos) { if (busca.isBlank()) emptyList() else Contatos.filtrar(contatos, busca).take(3) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        CampoBusca(busca, aoBuscar) {
            // Enter: o primeiro app; sem app, a primeira acao; sem nada, a web
            when {
                lista.isNotEmpty() -> acoes.abrir(lista.first())
                acoesBusca.isNotEmpty() -> aoAcao(acoesBusca.first())
                busca.isNotBlank() -> aoWeb(busca.trim())
            }
        }
        Row(Modifier.fillMaxSize()) {
            LazyColumn(state = estado, modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (comRecentes) item(key = "recentes") { Recentes(recentes, acoes) }
                if (agrupado) {
                    secoes.forEach { (letra, itens) ->
                        stickyHeader(key = "letra-$letra") {
                            Text(
                                letra,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                style = androidx.compose.ui.text.TextStyle(brush = degradeH()),
                                modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
                            )
                        }
                        itens.forEach { app -> item(key = app.chave) { LinhaApp(app, avisos[app.pacote] ?: 0, acoes, posicao[app.chave] ?: 0, abriu) } }
                    }
                } else {
                    acoesBusca.forEach { a -> item(key = "acao:" + a.titulo) { LinhaAcao(a.simbolo, a.titulo, a.sub) { aoAcao(a) } } }
                    lista.forEach { app -> item(key = app.chave) { LinhaApp(app, avisos[app.pacote] ?: 0, acoes, posicao[app.chave] ?: 0, abriu) } }
                    pessoas.forEach { c -> item(key = "pessoa:${c.id}") { LinhaPessoa(c, { aoLigar(c) }, { aoMensagem(c) }) } }
                    item(key = "web") { LinhaAcao("⌕", "Buscar “${busca.trim()}” na web", null) { aoWeb(busca.trim()) } }
                }
            }
            if (agrupado && secoes.size > 1) {
                AlfabetoLateral(
                    letras = secoes.keys.toList(),
                    aoEscolher = { letra -> indices[letra]?.let { escopo.launch { estado.scrollToItem(it) } } },
                    aoSoltar = {},
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaApp(app: AppInfo, avisos: Int, acoes: AcoesApp, indice: Int, abriu: Long) {
    var menu by remember(app.chave) { mutableStateOf(false) }
    // so anima a entrada nos primeiros instantes depois de abrir a lista
    val animar = remember { System.currentTimeMillis() - abriu < 450 }
    val toque = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val apertado by toque.collectIsPressedAsState()
    val escala by androidx.compose.animation.core.animateFloatAsState(if (apertado) 0.96f else 1f, label = "escala")
    Box(Modifier.entrada(indice.coerceAtMost(14), atraso = 24L, ligado = animar)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .combinedClickable(
                    interactionSource = toque,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = { acoes.abrir(app) },
                    onLongClick = { menu = true },
                )
                .padding(horizontal = 24.dp, vertical = 9.dp),
        ) {
            IconeApp(app, 40.dp)
            Spacer(Modifier.width(16.dp))
            Text(app.nome, fontSize = 18.sp, color = Texto, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            if (avisos > 0) Selo(avisos, Modifier.padding(start = 10.dp))
        }
        MenuApp(app, menu, { menu = false }, acoes)
    }
}

/** Os ultimos abertos, em icones grandes no topo da gaveta. */
@Composable
private fun Recentes(apps: List<AppInfo>, acoes: AcoesApp) {
    Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)) {
        Rotulo("Recentes", Modifier.padding(bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            apps.forEach { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp).clip(RoundedCornerShape(14.dp)).clickable { acoes.abrir(app) },
                ) {
                    IconeApp(app, 46.dp)
                    Text(
                        app.nome, color = Texto.copy(alpha = 0.8f), fontSize = 11.5.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * O campo de busca da lista de apps: uma linha fina (sem caixa), a lupa na cor da
 * paleta e o texto grande. Ja abre com o teclado (deslizar para cima e escrever).
 */
@Composable
private fun CampoBusca(texto: String, aoMudar: (String) -> Unit, aoEnviar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    val foco = remember { androidx.compose.ui.focus.FocusRequester() }
    var focado by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    val linha by androidx.compose.animation.core.animateFloatAsState(if (focado) 1f else 0.35f, label = "linha")
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Outlined.Search, contentDescription = null, tint = cor,
                modifier = Modifier.size(22.dp),
            )
            androidx.compose.foundation.text.BasicTextField(
                value = texto,
                onValueChange = aoMudar,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Texto, fontSize = 19.sp, fontWeight = FontWeight.Light),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(cor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { aoEnviar() }),
                decorationBox = { campo ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (texto.isEmpty()) Text("Buscar apps, contas, ajustes…", color = TextoFraco, fontSize = 19.sp, fontWeight = FontWeight.Light)
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
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Outlined.Close, contentDescription = "Limpar", tint = TextoFraco,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        // a linha embaixo: o degrade da paleta, mais forte com o teclado aberto
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .graphicsLayer { alpha = linha }
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(cor, MaterialTheme.colorScheme.secondary, androidx.compose.ui.graphics.Color.Transparent))),
        )
    }
}

/** Uma acao da busca (conta, timer, ajuste, web): o simbolo na cor, o titulo e a explicacao. */
@Composable
private fun LinhaAcao(simbolo: String, titulo: String, sub: String?, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Text(simbolo, color = cor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(titulo, color = Texto, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, color = TextoFraco, fontSize = 12.5.sp, maxLines = 1)
        }
    }
}

/** Uma pessoa achada na busca: a foto, o nome, o numero; tocar liga, o envelope manda mensagem. */
@Composable
private fun LinhaPessoa(c: Contato, aoLigar: () -> Unit, aoMensagem: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoLigar).padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Avatar(c.nome, 40.dp, c.foto)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(c.nome, color = Texto, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.numero, color = TextoFraco, fontSize = 12.5.sp, maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = aoMensagem),
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Message, contentDescription = "Mensagem", tint = cor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
