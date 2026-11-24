package org.wolfos.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.dados.Acao
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.EXEMPLOS
import org.wolfos.launcher.dados.filtrar
import org.wolfos.launcher.dados.interpretar
import org.wolfos.launcher.dados.semAcento

/** Um resultado da barra de comando. */
private sealed interface Resultado {
    val chave: String

    data class App(val app: AppInfo) : Resultado {
        override val chave get() = "app:" + app.chave
    }

    data class Pessoa(val contato: Contato, val mensagem: Boolean = false) : Resultado {
        override val chave get() = "pessoa:${contato.id}:$mensagem"
    }

    data class Feito(val acao: Acao) : Resultado {
        override val chave get() = "acao:" + acao.titulo
    }

    data object Ajustes : Resultado {
        override val chave get() = "ajustes"
    }

    data class Web(val termo: String) : Resultado {
        override val chave get() = "web"
    }
}

private val LIGAR = Regex("""^(ligar|liga|ligue|chamar)\s+(para\s+|pra\s+|pro\s+)?(.+)$""")
private val MENSAGEM = Regex("""^(mensagem|msg|sms|escrever)\s+(para\s+|pra\s+|pro\s+)?(.+)$""")

/** O que a barra de comando pede para a tela principal fazer. */
class AcoesComando(
    val ligar: (Contato) -> Unit,
    val mensagem: (Contato) -> Unit,
    val executar: (Acao) -> Unit,
    val ajustes: () -> Unit,
    val web: (String) -> Unit,
    val pedirContatos: () -> Unit,
    val fechar: () -> Unit,
)

/**
 * A barra de comando aberta. A barra fica colada no teclado e os resultados sobem a
 * partir dela: o melhor fica mais perto do polegar, e o Enter faz ele. Entende apps,
 * pessoas ("ligar mãe"), contas ("12 × 3"), timer, alarme, lanterna e ajustes do Android.
 */
@Composable
fun Comando(
    apps: List<AppInfo>,
    contatos: List<Contato>,
    temContatos: Boolean,
    texto: String,
    aoMudar: (String) -> Unit,
    acoesApp: AcoesApp,
    acoes: AcoesComando,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    val termo = texto.trim()
    val resultados = remember(apps, contatos, termo) { montar(apps, contatos, termo) }
    val executar: (Resultado) -> Unit = { r ->
        when (r) {
            is Resultado.App -> acoesApp.abrir(r.app)
            is Resultado.Pessoa -> if (r.mensagem) acoes.mensagem(r.contato) else acoes.ligar(r.contato)
            is Resultado.Feito -> acoes.executar(r.acao)
            Resultado.Ajustes -> acoes.ajustes()
            is Resultado.Web -> acoes.web(r.termo)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            // tocar fora dos resultados fecha
            .clickable(interactionSource = null, indication = null, onClick = acoes.fechar)
            .safeDrawingPadding()
            .padding(horizontal = 14.dp),
    ) {
        // reverseLayout: o primeiro resultado fica embaixo, colado na barra
        LazyColumn(
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            resultados.forEachIndexed { i, r ->
                item(key = r.chave) {
                    LinhaResultado(r, primeiro = i == 0, acoesApp, acoes) { executar(r) }
                }
            }
            if (!temContatos && termo.length >= 2) {
                item(key = "permissao") { Permissao(acoes.pedirContatos) }
            }
            if (termo.isEmpty()) {
                item(key = "exemplos") { Exemplos(temContatos, aoMudar, acoes.pedirContatos) }
            }
        }
        Barra(texto, aoMudar, foco, acoes.fechar) { resultados.firstOrNull()?.let(executar) }
    }
}

/** Junta tudo na ordem: o pedido explicito, as acoes, apps, pessoas, ajustes, web. */
private fun montar(apps: List<AppInfo>, contatos: List<Contato>, termo: String): List<Resultado> {
    if (termo.isEmpty()) return emptyList()
    val t = semAcento(termo)
    val lista = mutableListOf<Resultado>()
    LIGAR.find(t)?.let { m -> Contatos.filtrar(contatos, m.groupValues[3]).take(3).forEach { lista += Resultado.Pessoa(it) } }
    MENSAGEM.find(t)?.let { m -> Contatos.filtrar(contatos, m.groupValues[3]).take(3).forEach { lista += Resultado.Pessoa(it, true) } }
    val (ajustesAndroid, especiais) = interpretar(termo).partition { it.simbolo == "⚙" }
    especiais.forEach { lista += Resultado.Feito(it) }
    filtrar(apps, termo).take(6).forEach { lista += Resultado.App(it) }
    Contatos.filtrar(contatos, termo).take(3).forEach { lista += Resultado.Pessoa(it) }
    ajustesAndroid.forEach { lista += Resultado.Feito(it) }
    if ("ajustes do wolflauncher".contains(t) || "wolflauncher".startsWith(t)) lista += Resultado.Ajustes
    lista += Resultado.Web(termo)
    return lista.distinctBy { it.chave }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaResultado(r: Resultado, primeiro: Boolean, acoesApp: AcoesApp, acoes: AcoesComando, aoTocar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(18.dp)
    val (titulo, sub) = when (r) {
        is Resultado.App -> r.app.nome to null
        is Resultado.Pessoa -> (if (r.mensagem) "Mensagem para ${r.contato.nome}" else r.contato.nome) to r.contato.numero
        is Resultado.Feito -> r.acao.titulo to r.acao.sub
        Resultado.Ajustes -> "Ajustes do WolfLauncher" to null
        is Resultado.Web -> "Buscar “${r.termo}” na web" to null
    }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(forma)
                .then(if (primeiro) Modifier.background(cor.copy(alpha = 0.15f)).border(1.dp, cor.copy(alpha = 0.4f), forma) else Modifier)
                .combinedClickable(onClick = aoTocar, onLongClick = { if (r is Resultado.App) menu = true })
                .padding(horizontal = 14.dp)
                .height(58.dp),
        ) {
            when (r) {
                is Resultado.App -> IconeApp(r.app, 34.dp)
                is Resultado.Pessoa -> Avatar(r.contato.nome, foto = r.contato.foto)
                is Resultado.Feito -> Simbolo(r.acao.simbolo)
                Resultado.Ajustes -> Simbolo("⚙")
                is Resultado.Web -> Simbolo("⌕")
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    titulo,
                    color = Texto,
                    fontSize = 16.sp,
                    fontWeight = if (primeiro) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (sub != null) Text(sub, color = TextoFraco, fontSize = 12.sp, maxLines = 1)
            }
            if (r is Resultado.Pessoa && !r.mensagem) {
                // ligar e o toque na linha; a mensagem tem o botao dela
                Botaozinho("✉") { acoes.mensagem(r.contato) }
            }
            if (primeiro) Text("↵", color = cor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        if (r is Resultado.App) MenuApp(r.app, menu, { menu = false }, acoesApp)
    }
}

@Composable
private fun Simbolo(s: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(34.dp).clip(CircleShape).background(Superficie),
    ) { Text(s, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

/** As cores das pessoas sem foto (cada nome cai sempre na mesma). */
private val CORES_PESSOAS = listOf(
    Color(0xFFF5A432), Color(0xFF4C8DFF), Color(0xFF3DD68C), Color(0xFFB98CFF),
    Color(0xFFFF6B81), Color(0xFF34C6D3), Color(0xFFF5D547),
)

fun corDaPessoa(nome: String): Color = CORES_PESSOAS[(nome.trim().lowercase().hashCode() and 0x7fffffff) % CORES_PESSOAS.size]

/** Fotos da agenda, carregadas quando aparecem e guardadas num cache pequeno. */
private object Fotos {
    val cache = LruCache<String, ImageBitmap>(40)
}

/** A pessoa: a foto da agenda ou um circulo colorido com a inicial. */
@Composable
fun Avatar(nome: String, tamanho: Dp = 34.dp, foto: String? = null) {
    val contexto = LocalContext.current
    val imagem by produceState(foto?.let { Fotos.cache.get(it) }, foto) {
        if (value == null && foto != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    contexto.contentResolver.openInputStream(Uri.parse(foto))?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                }.getOrNull()
            }?.also { Fotos.cache.put(foto, it) }
        }
    }
    val cor = corDaPessoa(nome)
    val m = Modifier.size(tamanho).clip(CircleShape)
    imagem?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = m) } ?: Box(
        contentAlignment = Alignment.Center,
        modifier = m.background(cor.copy(alpha = 0.25f)),
    ) {
        val inicial = nome.trim().firstOrNull { it.isLetter() }?.uppercase() ?: "#"
        Text(inicial, color = cor, fontSize = (tamanho.value * 0.42f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Botaozinho(s: String, aoTocar: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = aoTocar),
    ) { Text(s, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp) }
}

@Composable
private fun Permissao(aoPermitir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Texto.copy(alpha = 0.05f))
            .clickable(onClick = aoPermitir)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Simbolo("☺")
        Text(
            "Permitir buscar pessoas da agenda",
            color = Texto,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        Text("Permitir", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Exemplos(temContatos: Boolean, aoUsar: (String) -> Unit, aoPermitir: () -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Rotulo("Também dá para escrever", Modifier.padding(start = 6.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EXEMPLOS.forEach { e -> Pilula(e, destaque = false) { aoUsar(e) } }
            Pilula(if (temContatos) "ligar mãe" else "ligar mãe (permitir agenda)", destaque = false) {
                if (temContatos) aoUsar("ligar mãe") else aoPermitir()
            }
        }
    }
}

@Composable
private fun Barra(texto: String, aoMudar: (String) -> Unit, foco: FocusRequester, aoFechar: () -> Unit, aoEnviar: () -> Unit) {
    val cor = MaterialTheme.colorScheme.primary
    // TextFieldValue so para o cursor ir para o fim quando o texto vem de fora (exemplos, leque)
    var valor by remember { mutableStateOf(TextFieldValue(texto, TextRange(texto.length))) }
    if (valor.text != texto) valor = TextFieldValue(texto, TextRange(texto.length))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Superficie)
            .border(1.dp, cor, CircleShape)
            // tocar na barra (fora do texto) nao fecha: so devolve o foco
            .clickable(interactionSource = null, indication = null) { foco.requestFocus() }
            .padding(start = 12.dp, end = 4.dp)
            .height(54.dp),
    ) {
        MarcaUivo()
        BasicTextField(
            value = valor,
            onValueChange = {
                valor = it
                if (it.text != texto) aoMudar(it.text)
            },
            singleLine = true,
            textStyle = TextStyle(color = Texto, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(cor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { aoEnviar() }),
            decorationBox = { campo ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (texto.isEmpty()) Text("Buscar ou mandar…", color = TextoFraco, fontSize = 15.sp)
                    campo()
                }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp).focusRequester(foco),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(46.dp).clip(CircleShape).clickable {
                if (texto.isEmpty()) aoFechar() else aoMudar("")
            },
        ) { Text("✕", color = TextoFraco, fontSize = 15.sp) }
    }
}
