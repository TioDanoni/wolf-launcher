package org.wolfos.launcher.ui

import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wolfos.launcher.R
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Os jeitos de mostrar a hora (a peca de mais impacto da tela inicial). */
enum class EstiloRelogio(val titulo: String) {
    VERTICAL("Vertical"),
    UIVO("Uivo"),
    MANUSCRITO("Manuscrito"),
    LUA("Lua"),
    NEON("Neon"),
    CLASSICO("Clássico"),
}

/** As fontes de cada estilo (OFL, do acervo do Google Fonts). */
val FonteUivo = FontFamily(Font(R.font.fonte_uivo))
val FonteManuscrito = FontFamily(Font(R.font.fonte_manuscrito))
val FonteLua = FontFamily(Font(R.font.fonte_lua))
val FonteNeon = FontFamily(Font(R.font.fonte_neon))

fun fonteDoEstilo(e: EstiloRelogio): FontFamily? = when (e) {
    EstiloRelogio.UIVO -> FonteUivo
    EstiloRelogio.MANUSCRITO -> FonteManuscrito
    EstiloRelogio.LUA -> FonteLua
    EstiloRelogio.NEON -> FonteNeon
    EstiloRelogio.CLASSICO, EstiloRelogio.VERTICAL -> null
}

private val PT = Locale.forLanguageTag("pt-BR")
private val HH = DateTimeFormatter.ofPattern("HH")
private val MM = DateTimeFormatter.ofPattern("mm")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val SEMANA = DateTimeFormatter.ofPattern("EEEE", PT)
private val SEMANA_CURTA = DateTimeFormatter.ofPattern("EEE", PT)
private val DATA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT)
private val DATA_CURTA = DateTimeFormatter.ofPattern("d MMM", PT)

/** Texto sem espaco sobrando em cima e embaixo (as fontes de display tem muito). */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun apertado(tamanho: TextUnit, fonte: FontFamily?, pincel: Brush, sombra: Shadow = SombraTexto) = TextStyle(
    fontFamily = fonte,
    fontSize = tamanho,
    lineHeight = tamanho * 0.92f,
    brush = pincel,
    shadow = sombra,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/**
 * Quanto a "tinta" da fonte ocupa em volta da linha de base (em fracao do tamanho):
 * medido em cada fonte. As fontes de display deixam muito espaco vazio na caixa da
 * linha (a Bebas usa so 60% dela); sem cortar, duas linhas ficam com um vao enorme.
 */
private class Tinta(val acima: Float, val abaixo: Float)

private val TINTA_UIVO = Tinta(0.74f, 0.04f)
private val TINTA_MANUSCRITO = Tinta(0.72f, 0.08f)
private val TINTA_LUA = Tinta(0.69f, 0.05f)
private val TINTA_NEON = Tinta(0.75f, 0.05f)

/** Mede o texto e diz que ele so ocupa a tinta (da linha de base para cima e para baixo). */
private fun Modifier.soATinta(t: Tinta, tamanho: TextUnit): Modifier = layout { medivel, limites ->
    val p = medivel.measure(limites.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val base = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: (p.height * 0.75f).toInt()
    val fs = tamanho.toPx()
    val topo = (base - t.acima * fs).roundToInt().coerceAtLeast(0)
    val altura = ((t.acima + t.abaixo) * fs).roundToInt().coerceIn(1, p.height)
    layout(p.width, altura) { p.place(0, -topo) }
}

/** Cada caractere desliza quando muda (o minuto virando). */
@Composable
private fun Digitos(texto: String, estilo: TextStyle, tinta: Tinta? = null) {
    Row {
        texto.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideInVertically(tween(450)) { -it / 2 } + fadeIn(tween(450))) togetherWith
                        (slideOutVertically(tween(450)) { it / 2 } + fadeOut(tween(300)))
                },
                label = "digito$i",
            ) { c ->
                Text(
                    c.toString(),
                    style = estilo,
                    modifier = if (tinta != null) Modifier.soATinta(tinta, estilo.fontSize) else Modifier,
                )
            }
        }
    }
}

/**
 * O relogio grande, no estilo escolhido. O Classico continua no Inicio; aqui ficam os
 * de impacto: Uivo (empilhado, condensado), Manuscrito (o dia a mao), Lua (serifa) e
 * Neon (arredondado com brilho).
 */
@Composable
fun RelogioGrande(estilo: EstiloRelogio, modifier: Modifier = Modifier, escala: Float = 1f) {
    val e = escala
    val momento = agora()
    val primaria = MaterialTheme.colorScheme.primary
    val segunda = MaterialTheme.colorScheme.secondary
    val claro = Brush.verticalGradient(0.2f to Texto, 1f to primaria)
    val cor = Brush.verticalGradient(listOf(primaria, segunda))
    val semana = momento.format(SEMANA).replaceFirstChar { it.uppercase() }
    val data = momento.format(DATA)

    when (estilo) {
        EstiloRelogio.UIVO -> Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
            Column {
                Digitos(momento.format(HH), apertado(96.sp * e, FonteUivo, claro), TINTA_UIVO)
                Box(Modifier.height(6.dp * e))
                Digitos(momento.format(MM), apertado(96.sp * e, FonteUivo, cor), TINTA_UIVO)
            }
            Column(Modifier.padding(start = 16.dp, bottom = 2.dp)) {
                Text(
                    semana.uppercase(),
                    style = TextStyle(fontFamily = FonteUivo, fontSize = 30.sp * e, letterSpacing = 0.06.em, brush = Brush.horizontalGradient(listOf(primaria, segunda)), shadow = SombraTexto),
                )
                Text(data, color = Texto.copy(alpha = 0.8f), fontSize = 15.sp, style = TextStyle(shadow = SombraTexto))
            }
        }

        EstiloRelogio.MANUSCRITO -> Column(modifier) {
            Text(
                momento.format(SEMANA_CURTA).replaceFirstChar { it.uppercase() }.let { if (it.endsWith('.')) it else "$it." },
                style = apertado(84.sp * e, FonteManuscrito, Brush.linearGradient(listOf(Texto, primaria))),
                modifier = Modifier.soATinta(TINTA_MANUSCRITO, 84.sp * e).rotate(-4f).offset(x = (-2).dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp, start = 6.dp)) {
                Digitos(momento.format(HHMM), TextStyle(fontFamily = FonteManuscrito, fontSize = 28.sp, color = Texto, shadow = SombraTexto))
                Text(
                    "  ·  " + momento.format(DATA_CURTA).replace(".", ""),
                    style = TextStyle(fontFamily = FonteManuscrito, fontSize = 24.sp, color = Texto.copy(alpha = 0.85f), shadow = SombraTexto),
                )
            }
        }

        EstiloRelogio.LUA -> Column(modifier) {
            Digitos(momento.format(HHMM), apertado(80.sp * e, FonteLua, claro), TINTA_LUA)
            Text(
                "$semana, $data",
                style = TextStyle(fontFamily = FonteLua, fontSize = 22.sp, brush = Brush.horizontalGradient(listOf(primaria, segunda)), shadow = SombraTexto),
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )
        }

        EstiloRelogio.NEON -> Column(modifier) {
            // o brilho e uma sombra borrada na cor da paleta
            val brilho = Shadow(primaria.copy(alpha = 0.85f), Offset(0f, 0f), 36f)
            Digitos(momento.format(HHMM), apertado(78.sp * e, FonteNeon, Brush.horizontalGradient(listOf(primaria, segunda)), brilho), TINTA_NEON)
            Text(
                "${momento.format(SEMANA_CURTA).replace(".", "").uppercase()}  ·  ${momento.format(DATA_CURTA).replace(".", "").uppercase()}",
                style = TextStyle(fontFamily = FonteNeon, fontSize = 18.sp, letterSpacing = 0.2.em, color = Texto, shadow = Shadow(segunda.copy(alpha = 0.7f), Offset.Zero, 18f)),
                modifier = Modifier.padding(start = 6.dp, top = 12.dp),
            )
        }

        // a hora grande e fina, o dia da semana embaixo e a data com um traco na cor da paleta
        EstiloRelogio.VERTICAL -> Column(modifier) {
            Digitos(
                momento.format(HHMM),
                TextStyle(
                    fontSize = 84.sp * e,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                    lineHeight = 84.sp * e,
                    brush = Brush.verticalGradient(0.55f to Texto, 1f to Texto.copy(alpha = 0.82f)),
                    shadow = SombraTexto,
                ),
            )
            Text(
                semana,
                style = TextStyle(fontSize = 28.sp * e, color = Texto, shadow = SombraTexto),
                modifier = Modifier.padding(start = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) {
                Box(Modifier.size(width = 3.dp, height = 18.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Brush.verticalGradient(listOf(primaria, segunda))))
                Text(data, color = Texto.copy(alpha = 0.75f), fontSize = 16.sp, style = TextStyle(shadow = SombraTexto), modifier = Modifier.padding(start = 10.dp))
                // a bateria ao lado da data (le de novo a cada minuto, junto com o relogio)
                val contexto = androidx.compose.ui.platform.LocalContext.current
                val bateria = androidx.compose.runtime.remember(momento.minute) { org.wolfos.launcher.dados.Bateria.estado(contexto) }
                bateria?.let { (pct, carregando) ->
                    Text("   ·   ", color = Texto.copy(alpha = 0.4f), fontSize = 16.sp)
                    if (carregando) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Outlined.Bolt, contentDescription = "Carregando",
                            tint = primaria, modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        "$pct%",
                        color = if (pct <= 15 && !carregando) androidx.compose.ui.graphics.Color(0xFFFF6B6B) else Texto.copy(alpha = 0.75f),
                        fontSize = 16.sp, style = TextStyle(shadow = SombraTexto),
                    )
                }
            }
        }

        EstiloRelogio.CLASSICO -> Unit
    }
}
