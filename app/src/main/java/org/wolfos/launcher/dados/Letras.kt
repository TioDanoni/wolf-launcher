package org.wolfos.launcher.dados

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Um ponto do traco do dedo. */
data class Ponto(val x: Float, val y: Float)

/**
 * Reconhece a letra desenhada com um traco so (sem tirar o dedo). E o metodo "$1"
 * simplificado: o traco e reamostrado em pontos iguais, posto na mesma escala e
 * comparado ponto a ponto com modelos de cada letra. Sem rotacao: um N deitado nao
 * vira Z. Leve, sem rede e sem modelo para baixar.
 */
object Letras {
    private const val N = 48

    /** As letras mais provaveis, da melhor para a pior. */
    fun reconhecer(traco: List<Ponto>, quantas: Int = 3): List<Char> {
        if (traco.size < 4) return emptyList()
        val p = normalizar(traco)
        return modelos
            .map { (letra, m) -> letra to distancia(p, m) }
            .sortedBy { it.second }
            .map { it.first }
            .distinct()
            .take(quantas)
    }

    /** Um traco quase reto e um deslizar, nao uma letra. */
    fun ehReto(traco: List<Ponto>): Boolean {
        if (traco.size < 2) return true
        val corda = dist(traco.first(), traco.last())
        return corda / max(comprimento(traco), 1f) > 0.92f
    }

    fun comprimento(traco: List<Ponto>): Float =
        traco.zipWithNext { a, b -> dist(a, b) }.sum()

    // ------------------------------------------------------------ o metodo

    private fun dist(a: Ponto, b: Ponto) = hypot(a.x - b.x, a.y - b.y)

    private fun distancia(a: List<Ponto>, b: List<Ponto>): Float =
        a.indices.sumOf { dist(a[it], b[it]).toDouble() }.toFloat() / a.size

    private fun normalizar(traco: List<Ponto>): List<Ponto> {
        val r = reamostrar(traco)
        val minX = r.minOf { it.x }
        val maxX = r.maxOf { it.x }
        val minY = r.minOf { it.y }
        val maxY = r.maxOf { it.y }
        // escala igual nos dois eixos (um I fino continua fino) e centro na origem
        val escala = 100f / max(max(maxX - minX, maxY - minY), 1f)
        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        return r.map { Ponto((it.x - cx) * escala, (it.y - cy) * escala) }
    }

    private fun reamostrar(traco: List<Ponto>): List<Ponto> {
        val passo = comprimento(traco) / (N - 1)
        if (passo <= 0f) return List(N) { traco.first() }
        val pts = traco.toMutableList()
        val saida = mutableListOf(pts.first())
        var acumulado = 0f
        var i = 1
        while (i < pts.size) {
            val d = dist(pts[i - 1], pts[i])
            if (acumulado + d >= passo && d > 0f) {
                val t = (passo - acumulado) / d
                val q = Ponto(pts[i - 1].x + t * (pts[i].x - pts[i - 1].x), pts[i - 1].y + t * (pts[i].y - pts[i - 1].y))
                saida += q
                pts.add(i, q)
                acumulado = 0f
            } else {
                acumulado += d
            }
            i++
        }
        while (saida.size < N) saida += pts.last()
        return saida.take(N)
    }

    // ------------------------------------------------------------ os modelos

    /** Arco de circulo, em graus (0 = direita, 90 = baixo, a tela tem y para baixo). */
    private fun arco(cx: Float, cy: Float, r: Float, de: Float, ate: Float, passos: Int = 16): List<Pair<Float, Float>> =
        (0..passos).map {
            val a = Math.toRadians((de + (ate - de) * it / passos).toDouble())
            (cx + r * cos(a).toFloat()) to (cy + r * sin(a).toFloat())
        }

    private fun p(vararg v: Int): List<Pair<Float, Float>> = v.toList().chunked(2).map { (x, y) -> x.toFloat() to y.toFloat() }

    /**
     * Os jeitos comuns de escrever cada letra com um traco so (maiuscula ou de mao).
     * Cada modelo entra tambem de tras para frente: a forma e a mesma.
     */
    private val desenhos: Map<Char, List<List<Pair<Float, Float>>>> = mapOf(
        'A' to listOf(p(0, 100, 50, 0, 100, 100), p(0, 100, 50, 0, 100, 100, 75, 55, 25, 55)),
        'B' to listOf(
            p(0, 100, 0, 0, 55, 3, 75, 22, 55, 47, 0, 50, 65, 55, 85, 77, 62, 98, 0, 100),
            p(0, 0, 0, 100, 0, 0, 55, 3, 75, 22, 55, 47, 0, 50, 65, 55, 85, 77, 62, 98, 0, 100),
        ),
        'C' to listOf(arco(50f, 50f, 50f, -40f, -320f)),
        'D' to listOf(p(0, 100, 0, 0, 45, 5, 85, 35, 85, 65, 45, 95, 0, 100), p(0, 0, 0, 100, 0, 0, 45, 5, 85, 35, 85, 65, 45, 95, 0, 100)),
        'E' to listOf(
            p(90, 0, 0, 0, 0, 50, 60, 50, 0, 50, 0, 100, 90, 100),
            arco(50f, 25f, 25f, -30f, -270f, 8) + arco(50f, 75f, 25f, -90f, -330f, 8),
        ),
        'F' to listOf(p(90, 0, 0, 0, 0, 100), p(90, 0, 0, 0, 0, 50, 60, 50, 0, 50, 0, 100)),
        'G' to listOf(arco(50f, 50f, 50f, -40f, -360f) + p(100, 55, 55, 55)),
        'H' to listOf(p(0, 0, 0, 100, 0, 55, 45, 40, 85, 55, 85, 100), p(0, 0, 0, 100, 0, 50, 100, 50, 100, 0, 100, 100)),
        'I' to listOf(p(50, 0, 50, 100)),
        'J' to listOf(p(90, 0, 90, 70) + arco(55f, 70f, 35f, 0f, 180f, 8)),
        'K' to listOf(p(100, 0, 0, 50, 100, 100), p(0, 0, 0, 100, 0, 55, 90, 0, 0, 55, 90, 100)),
        'L' to listOf(p(0, 0, 0, 100, 80, 100)),
        'M' to listOf(p(0, 100, 0, 0, 50, 60, 100, 0, 100, 100), p(0, 100, 20, 0, 50, 70, 80, 0, 100, 100)),
        'N' to listOf(p(0, 100, 0, 0, 100, 100, 100, 0)),
        'O' to listOf(arco(50f, 50f, 50f, -90f, -450f, 24), arco(50f, 50f, 50f, -90f, 270f, 24)),
        'P' to listOf(p(0, 100, 0, 0, 55, 0, 85, 22, 55, 48, 0, 50), p(0, 0, 0, 100, 0, 0, 55, 0, 85, 22, 55, 48, 0, 50)),
        'Q' to listOf(arco(50f, 50f, 50f, -90f, -450f, 24) + p(65, 75, 100, 110)),
        'R' to listOf(p(0, 100, 0, 0, 55, 0, 85, 22, 55, 48, 0, 50, 90, 100), p(0, 0, 0, 100, 0, 0, 55, 0, 85, 22, 55, 48, 0, 50, 90, 100)),
        'S' to listOf(arco(50f, 25f, 25f, -20f, -270f, 10) + arco(50f, 75f, 25f, -90f, 160f, 10)),
        'T' to listOf(p(0, 0, 100, 0, 50, 0, 50, 100), p(0, 0, 100, 0, 50, 0, 50, 100).reversed()),
        'U' to listOf(p(0, 0, 0, 60) + arco(50f, 60f, 50f, 180f, 0f, 10) + p(100, 0)),
        'V' to listOf(p(0, 0, 50, 100, 100, 0)),
        'W' to listOf(p(0, 0, 25, 100, 50, 30, 75, 100, 100, 0)),
        'X' to listOf(p(0, 0, 100, 100, 100, 0, 0, 100)),
        'Y' to listOf(p(0, 0, 50, 50, 100, 0, 50, 50, 50, 100), p(0, 0, 50, 50, 100, 0, 50, 50, 30, 100)),
        'Z' to listOf(p(0, 0, 100, 0, 0, 100, 100, 100)),
    )

    private val modelos: List<Pair<Char, List<Ponto>>> = desenhos.flatMap { (letra, formas) ->
        formas.flatMap { f ->
            val pts = f.map { (x, y) -> Ponto(x, y) }
            listOf(letra to normalizar(pts), letra to normalizar(pts.reversed()))
        }
    }
}
