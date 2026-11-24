package org.wolfos.launcher.dados

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Uma acao pronta que a barra de comando entendeu ("timer 5 min", "12 x 3", "wi-fi"). */
class Acao(
    val simbolo: String,
    val titulo: String,
    val sub: String,
    /** Algo que a tela principal faz (ex.: "insights" abre o Wolf Insights). */
    val especial: String? = null,
    val executar: (Context) -> Unit,
)

/** Os atalhos de exemplo que aparecem com a barra vazia. */
val EXEMPLOS = listOf("timer 20 min", "modo trabalho", "caça 45 min", "pomodoro", "nota comprar filamento", "tarefa ligar pro banco", "150 * 0.15", "lanterna", "wi-fi")

/**
 * Entende o que foi escrito na barra de comando. Devolve as acoes na ordem de
 * importancia (a primeira fica colada na barra e e a que o Enter faz).
 */
fun interpretar(texto: String): List<Acao> {
    val t = semAcento(texto.trim())
    if (t.isEmpty()) return emptyList()
    val acoes = mutableListOf<Acao>()
    notaOuTarefa(texto.trim())?.let(acoes::add)
    if (listOf("quanto usei", "tempo de tela", "insights", "estatistica", "uso do celular").any { t.contains(it) }) {
        acoes += Acao("▥", "Wolf Insights", "tempo de tela, apps que mais usa e a semana", especial = "insights") { }
    }
    if ("pomodoro".startsWith(t) && t.length >= 4) {
        acoes += Acao("◷", "Pomodoro de 25 min", "foco; o Painel mostra quanto falta") { ctx -> TimerWolf(ctx).comecar(ctx, 25, "Pomodoro") }
    }
    acoes += rotinaOuCaca(t)
    timer(t)?.let(acoes::add)
    alarme(t)?.let(acoes::add)
    conta(texto.trim())?.let(acoes::add)
    if (t.length >= 3 && "lanterna".startsWith(t)) {
        acoes += Acao("ϟ", if (Lanterna.ligada) "Desligar a lanterna" else "Ligar a lanterna", "lanterna do celular", executar = Lanterna::alternar)
    }
    acoes += ajustesDoAndroid(t)
    return acoes
}

// ---------------------------------------------------------------- rotinas e Modo Caca

private val CACA = Regex("""^(modo\s*)?(caca|foco|focar)\s*(\d+)?\s*(m|min|minutos?|h|horas?)?$""")

private fun rotinaOuCaca(t: String): List<Acao> {
    val acoes = mutableListOf<Acao>()
    // "modo trabalho", "trabalho", "modo casa"...
    val sem = t.removePrefix("modo").trim()
    if (sem.length >= 3) {
        Rotina.entries.filter { semAcento(it.titulo).startsWith(sem) && (t.startsWith("modo") || sem.length >= 5) }.forEach { r ->
            acoes += Acao("◐", "Modo ${r.titulo}", "a tela inicial sugere os apps de ${r.titulo.lowercase()}", especial = "rotina") { ctx ->
                Rotinas(ctx).manual = r
            }
        }
    }
    if (t.length >= 6 && listOf("modo normal", "sair do modo", "desligar modo", "sem rotina").any { it.startsWith(t) || t.startsWith(it) }) {
        acoes += Acao("◑", "Sair da rotina", "volta ao normal (as automáticas seguem o horário)", especial = "rotina") { ctx ->
            Rotinas(ctx).manual = null
        }
    }
    CACA.find(t)?.let { m ->
        val n = m.groupValues[3].toIntOrNull()
        val minutos = when {
            n == null -> 45
            m.groupValues[4].startsWith("h") -> n * 60
            else -> n
        }.coerceIn(1, 12 * 60)
        acoes += Acao("◉", "Modo Caça por ${if (minutos % 60 == 0) "${minutos / 60} h" else "$minutos min"}", "esconde as distrações; só o essencial fica", especial = "rotina") { ctx ->
            ModoCaca(ctx).comecar(minutos)
        }
    }
    if (listOf("parar caca", "sair da caca", "parar foco", "sair do foco").any { it.startsWith(t) && t.length >= 7 }) {
        acoes += Acao("◎", "Encerrar o Modo Caça", "as distrações voltam", especial = "rotina") { ctx -> ModoCaca(ctx).parar() }
    }
    return acoes
}

// ---------------------------------------------------------------- timer e alarme

private val TIMER = Regex("""^(timer|temporizador|cronometro|contar)\s*(\d+)\s*(s|seg|segundos?|m|min|minutos?|h|horas?)?$""")
private val ALARME = Regex("""^(alarme|despertador|acordar|acorda)\s*(as\s*)?(\d{1,2})(?:\s*[:h]\s*(\d{2}))?\s*h?$""")

private fun timer(t: String): Acao? {
    val m = TIMER.find(t) ?: return null
    val n = m.groupValues[2].toIntOrNull() ?: return null
    val unidade = m.groupValues[3]
    val segundos = when {
        unidade.startsWith("s") -> n
        unidade.startsWith("h") -> n * 3600
        else -> n * 60
    }
    if (segundos !in 1..86_400) return null
    val texto = when {
        segundos % 3600 == 0 -> "${segundos / 3600} h"
        segundos % 60 == 0 -> "${segundos / 60} min"
        else -> "$segundos s"
    }
    return Acao("◷", "Timer de $texto", "começa agora; o Painel mostra quanto falta") { ctx ->
        TimerWolf(ctx).comecarSegundos(ctx, segundos, "Timer de $texto")
    }
}

private fun alarme(t: String): Acao? {
    val m = ALARME.find(t) ?: return null
    val hora = m.groupValues[3].toIntOrNull() ?: return null
    val minuto = m.groupValues[4].ifEmpty { "0" }.toInt()
    if (hora !in 0..23 || minuto !in 0..59) return null
    val texto = "%02d:%02d".format(hora, minuto)
    return Acao("⏰", "Alarme para $texto", "no relógio do Android") { ctx ->
        Apps.abrirTela(
            ctx,
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hora)
                .putExtra(AlarmClock.EXTRA_MINUTES, minuto)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false),
        )
    }
}

// ---------------------------------------------------------------- notas e tarefas

private val NOTA = Regex("""^(nota|anotar|anota)\s+(.+)$""", RegexOption.IGNORE_CASE)
private val TAREFA = Regex("""^(tarefa|lembrar|lembrete|fazer|todo)\s+(de\s+)?(.+)$""", RegexOption.IGNORE_CASE)

/** "nota comprar filamento" vira uma nota; "tarefa ligar pro banco", uma tarefa (com caixinha). */
private fun notaOuTarefa(texto: String): Acao? {
    NOTA.find(texto)?.let { m ->
        val t = m.groupValues[2].trim()
        return Acao("✎", "Nota: $t", "guarda nas notas do Painel") { ctx -> Notas(ctx).adicionar(t, tarefa = false) }
    }
    TAREFA.find(texto)?.let { m ->
        val t = m.groupValues[3].trim()
        return Acao("☐", "Tarefa: $t", "entra na lista de tarefas do Painel") { ctx -> Notas(ctx).adicionar(t, tarefa = true) }
    }
    return null
}

// ---------------------------------------------------------------- contas

private val NUMERO = DecimalFormat("#,##0.##########", DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")))

/** "12 × 3", "1.500 / 4", "2^10", "15% de 80": so aparece se for mesmo uma conta. */
private fun conta(texto: String): Acao? {
    val porcento = Regex("""^\s*([\d.,]+)\s*%\s*de\s*([\d.,]+)\s*$""").find(texto)
    val expressao = if (porcento != null) {
        "(${porcento.groupValues[1]})/100*(${porcento.groupValues[2]})"
    } else {
        if (!texto.any { it in "+-*/x×÷^" } || !texto.any(Char::isDigit)) return null
        if (!texto.all { it.isDigit() || it in " .,+-*/x×÷^()" }) return null
        texto
    }
    val valor = runCatching { Calculadora(expressao).calcular() }.getOrNull() ?: return null
    if (valor.isNaN() || valor.isInfinite()) return null
    val resultado = NUMERO.format(valor)
    return Acao("=", resultado, "tocar copia o resultado") { ctx ->
        val area = ctx.getSystemService(ClipboardManager::class.java)
        area?.setPrimaryClip(ClipData.newPlainText("resultado", resultado))
    }
}

/** Calculadora pequena: + - * / ^ e parenteses, virgula como decimal (1.500,5). */
private class Calculadora(texto: String) {
    private val s = texto
        .replace(" ", "")
        .replace('x', '*').replace('×', '*').replace('÷', '/')
        // 1.500,5 -> 1500.5 (ponto de milhar, virgula decimal); 2.5 continua 2.5
        .let { if (it.contains(',')) it.replace(".", "").replace(',', '.') else it }
    private var i = 0

    fun calcular(): Double {
        val v = soma()
        require(i == s.length)
        return v
    }

    private fun soma(): Double {
        var v = produto()
        while (i < s.length && (s[i] == '+' || s[i] == '-')) {
            val op = s[i++]
            val d = produto()
            v = if (op == '+') v + d else v - d
        }
        return v
    }

    private fun produto(): Double {
        var v = potencia()
        while (i < s.length && (s[i] == '*' || s[i] == '/')) {
            val op = s[i++]
            val d = potencia()
            v = if (op == '*') v * d else v / d
        }
        return v
    }

    private fun potencia(): Double {
        val base = unario()
        if (i < s.length && s[i] == '^') {
            i++
            return Math.pow(base, potencia())
        }
        return base
    }

    private fun unario(): Double = when {
        i < s.length && s[i] == '-' -> { i++; -unario() }
        i < s.length && s[i] == '+' -> { i++; unario() }
        i < s.length && s[i] == '(' -> {
            i++
            val v = soma()
            require(i < s.length && s[i] == ')')
            i++
            v
        }
        else -> {
            val inicio = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            s.substring(inicio, i).toDouble()
        }
    }
}

// ---------------------------------------------------------------- ajustes do Android

private class Ajuste(val nome: String, val palavras: List<String>, val intent: () -> Intent)

private val AJUSTES = listOf(
    Ajuste("Wi-Fi", listOf("wifi", "wi-fi", "internet", "rede")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)
    },
    Ajuste("Internet e dados móveis", listOf("dados", "internet", "4g", "5g", "chip")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_WIRELESS_SETTINGS)
    },
    Ajuste("Bluetooth", listOf("bluetooth", "fone", "parear")) { Intent(Settings.ACTION_BLUETOOTH_SETTINGS) },
    Ajuste("Volume", listOf("volume", "som")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)
    },
    Ajuste("Som e vibração", listOf("som", "toque", "vibracao", "silencio")) { Intent(Settings.ACTION_SOUND_SETTINGS) },
    Ajuste("Tela e brilho", listOf("tela", "brilho", "modo escuro", "fonte")) { Intent(Settings.ACTION_DISPLAY_SETTINGS) },
    Ajuste("Bateria", listOf("bateria", "economia")) { Intent(Intent.ACTION_POWER_USAGE_SUMMARY) },
    Ajuste("Armazenamento", listOf("armazenamento", "espaco", "memoria")) { Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) },
    Ajuste("Apps", listOf("apps", "aplicativos")) { Intent(Settings.ACTION_APPLICATION_SETTINGS) },
    Ajuste("Localização", listOf("localizacao", "gps")) { Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
    Ajuste("Modo avião", listOf("aviao", "modo aviao")) { Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) },
    Ajuste("Data e hora", listOf("data", "hora", "fuso")) { Intent(Settings.ACTION_DATE_SETTINGS) },
    Ajuste("Idioma", listOf("idioma", "lingua")) { Intent(Settings.ACTION_LOCALE_SETTINGS) },
    Ajuste("Teclado", listOf("teclado")) { Intent(Settings.ACTION_INPUT_METHOD_SETTINGS) },
    Ajuste("Segurança", listOf("seguranca", "senha", "bloqueio", "digital")) { Intent(Settings.ACTION_SECURITY_SETTINGS) },
    Ajuste("Privacidade", listOf("privacidade", "permissoes")) { Intent(Settings.ACTION_PRIVACY_SETTINGS) },
    Ajuste("Acessibilidade", listOf("acessibilidade")) { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
    Ajuste("Transmitir tela", listOf("transmitir", "cast", "espelhar")) { Intent(Settings.ACTION_CAST_SETTINGS) },
    Ajuste("NFC e pagamento", listOf("nfc", "pagamento", "aproximacao")) { Intent(Settings.ACTION_NFC_SETTINGS) },
    Ajuste("App de tela inicial", listOf("tela inicial", "launcher", "padrao")) { Intent(Settings.ACTION_HOME_SETTINGS) },
    Ajuste("Opções do desenvolvedor", listOf("desenvolvedor", "dev")) { Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
    Ajuste("Configurações", listOf("configuracoes", "ajustes", "config")) { Intent(Settings.ACTION_SETTINGS) },
)

private fun ajustesDoAndroid(t: String): List<Acao> {
    if (t.length < 2) return emptyList()
    return AJUSTES
        .filter { a -> semAcento(a.nome).contains(t) || a.palavras.any { it.startsWith(t) } }
        .take(3)
        .map { a -> Acao("⚙", a.nome, "ajuste do Android") { ctx -> Apps.abrirTela(ctx, a.intent()) } }
}

// ---------------------------------------------------------------- lanterna

/** A lanterna (nao precisa de permissao: so usa o flash, nao a camera). */
object Lanterna {
    @Volatile
    var ligada = false
        private set
    private var acompanhando = false

    fun alternar(contexto: Context) {
        val cameras = contexto.getSystemService(CameraManager::class.java) ?: return
        runCatching {
            val id = cameras.cameraIdList.firstOrNull {
                cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            if (!acompanhando) {
                acompanhando = true
                // acompanha quando outro app (ou o painel rapido) liga ou desliga
                cameras.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, ligou: Boolean) {
                        if (cameraId == id) ligada = ligou
                    }
                }, null)
            }
            cameras.setTorchMode(id, !ligada)
            ligada = !ligada
        }
    }
}
