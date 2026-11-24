package org.wolfos.launcher.dados

import android.content.Context

/** Os ajustes do launcher (os que nao sao de um app especifico). */
class Preferencias(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Baixar e instalar a versao nova sozinho (senao, so avisa). */
    var instalarSozinho: Boolean
        get() = prefs.getBoolean("instalar_sozinho", true)
        set(v) = prefs.edit().putBoolean("instalar_sozinho", v).apply()

    /** Tocar duas vezes num espaco vazio bloqueia a tela. */
    var doisToques: Boolean
        get() = prefs.getBoolean("dois_toques", true)
        set(v) = prefs.edit().putBoolean("dois_toques", v).apply()

    /** O player de musica na tela inicial (quando algo toca). */
    var mostrarPlayer: Boolean
        get() = prefs.getBoolean("mostrar_player", true)
        set(v) = prefs.edit().putBoolean("mostrar_player", v).apply()

    /** Os compromissos de hoje embaixo do relogio. */
    var compromissos: Boolean
        get() = prefs.getBoolean("compromissos", true)
        set(v) = prefs.edit().putBoolean("compromissos", v).apply()

    /** A aurora do fundo se mexendo devagar. */
    var fundoAnimado: Boolean
        get() = prefs.getBoolean("fundo_animado", true)
        set(v) = prefs.edit().putBoolean("fundo_animado", v).apply()

    /** A paleta de cores (nome do enum Paleta). */
    var paleta: String
        get() = prefs.getString("paleta", "LOBO") ?: "LOBO"
        set(v) = prefs.edit().putString("paleta", v).apply()

    /** O estilo do relogio (nome do enum EstiloRelogio). */
    var estiloRelogio: String
        get() = prefs.getString("estilo_relogio", "UIVO") ?: "UIVO"
        set(v) = prefs.edit().putString("estilo_relogio", v).apply()

    /** A fileira da Matilha na tela inicial. */
    var mostrarMatilha: Boolean
        get() = prefs.getBoolean("mostrar_matilha", true)
        set(v) = prefs.edit().putBoolean("mostrar_matilha", v).apply()

    /** A fileira "Agora" do painel de apps (sugestoes pela hora e pelo uso). */
    var sugestoes: Boolean
        get() = prefs.getBoolean("sugestoes", true)
        set(v) = prefs.edit().putBoolean("sugestoes", v).apply()

    var modoNoite: Boolean
        get() = prefs.getBoolean("modo_noite", true)
        set(v) = prefs.edit().putBoolean("modo_noite", v).apply()

    /** A janela do modo noite, em minutos do dia (22:30 = 1350). */
    var noiteInicio: Int
        get() = prefs.getInt("noite_inicio", 22 * 60 + 30)
        set(v) = prefs.edit().putInt("noite_inicio", v).apply()

    var noiteFim: Int
        get() = prefs.getInt("noite_fim", 7 * 60)
        set(v) = prefs.edit().putInt("noite_fim", v).apply()

    /** Ja pediu uma vez, sozinho, para ser a tela inicial (depois so pela faixa). */
    var jaPediuPadrao: Boolean
        get() = prefs.getBoolean("ja_pediu_padrao", false)
        set(v) = prefs.edit().putBoolean("ja_pediu_padrao", v).apply()

    var ultimaVerificacao: Long
        get() = prefs.getLong("ultima_verificacao", 0L)
        set(v) = prefs.edit().putLong("ultima_verificacao", v).apply()
}
