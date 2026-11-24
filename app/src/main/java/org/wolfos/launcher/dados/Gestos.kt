package org.wolfos.launcher.dados

/** Os gestos da tela inicial. */
enum class Gesto(val titulo: String) {
    CIMA("Deslizar para cima"),
    BAIXO("Deslizar para baixo"),
    DIREITA("Deslizar para a direita"),
    ESQUERDA("Deslizar para a esquerda"),
    DOIS_TOQUES("Tocar duas vezes"),
}

/** O que um gesto pode fazer. */
enum class AcaoGesto(val titulo: String) {
    BUSCA("Buscador"),
    TODOS_APPS("Todos os apps"),
    NOTIFICACOES("Notificações"),
    DIA("Seu dia"),
    WIDGETS("Painel"),
    BLOQUEAR("Bloquear a tela"),
    NADA("Nada"),
}

object Gestos {
    /** O que cada gesto faz quando a pessoa nao escolheu. */
    val PADRAO = mapOf(
        Gesto.CIMA to AcaoGesto.BUSCA,
        Gesto.BAIXO to AcaoGesto.NOTIFICACOES,
        Gesto.DIREITA to AcaoGesto.DIA,
        Gesto.ESQUERDA to AcaoGesto.WIDGETS,
        Gesto.DOIS_TOQUES to AcaoGesto.BLOQUEAR,
    )
}
