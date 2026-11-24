package org.wolfos.launcher.dados

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.ZoneId

/** Um periodo de uso do celular: tela ligada de um desbloqueio ate apagar. */
@Immutable
data class Sessao(val inicio: Long, val fim: Long, val pacotes: List<String>) {
    val minutos: Int get() = ((fim - inicio) / 60_000).toInt()
}

/** O resumo de um dia de uso. */
@Immutable
data class ResumoDia(
    val data: LocalDate,
    /** Tempo com algum app na frente (sem contar o launcher), em ms. */
    val tempoDeTela: Long,
    val desbloqueios: Int,
    val primeiroDesbloqueio: Long?,
    val sessoes: List<Sessao>,
    /** Tempo de cada app, em ms. */
    val porApp: Map<String, Long>,
    /** Tempo de uso em cada hora do dia (0 a 23), em ms. */
    val porHora: LongArray,
)

/**
 * Wolf Insights: o uso do celular, lido do proprio Android (UsageStats), so no
 * aparelho. Precisa do "acesso ao uso", que a pessoa liga se quiser.
 */
object Insights {
    fun temPermissao(contexto: Context): Boolean = runCatching {
        val ops = contexto.getSystemService(AppOpsManager::class.java)
        val modo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), contexto.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), contexto.packageName)
        }
        modo == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun pedirPermissao(contexto: Context) {
        val direto = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + contexto.packageName))
        if (!Apps.abrirTela(contexto, direto)) Apps.abrirTela(contexto, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    /** O resumo de um dia (hoje = de meia-noite ate agora). */
    fun dia(contexto: Context, data: LocalDate): ResumoDia? {
        if (!temPermissao(contexto)) return null
        val usm = contexto.getSystemService(UsageStatsManager::class.java) ?: return null
        val zona = ZoneId.systemDefault()
        val inicio = data.atStartOfDay(zona).toInstant().toEpochMilli()
        val fim = minOf(data.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli(), System.currentTimeMillis())
        if (fim <= inicio) return null
        val eventos = runCatching { usm.queryEvents(inicio, fim) }.getOrNull() ?: return null
        val ignorar = setOf(contexto.packageName, "com.android.systemui")

        val porApp = HashMap<String, Long>()
        val porHora = LongArray(24)
        var desbloqueios = 0
        var primeiro: Long? = null
        val sessoes = mutableListOf<Sessao>()
        var frente: String? = null
        var desde = 0L
        var sessaoInicio = 0L
        val sessaoApps = LinkedHashSet<String>()

        fun somar(pacote: String, de: Long, ate: Long) {
            if (ate <= de || pacote in ignorar) return
            porApp[pacote] = (porApp[pacote] ?: 0L) + (ate - de)
            // espalha o tempo pelas horas do dia
            var t = de
            while (t < ate) {
                val h = java.time.Instant.ofEpochMilli(t).atZone(zona).hour
                val proximaHora = java.time.Instant.ofEpochMilli(t).atZone(zona).withMinute(0).withSecond(0).withNano(0).plusHours(1).toInstant().toEpochMilli()
                val pedaco = minOf(ate, proximaHora) - t
                porHora[h] += pedaco
                t += pedaco
            }
        }

        fun fecharSessao(quando: Long) {
            if (sessaoInicio > 0 && quando - sessaoInicio >= 60_000) {
                sessoes += Sessao(sessaoInicio, quando, sessaoApps.toList())
            }
            sessaoInicio = 0L
            sessaoApps.clear()
        }

        val e = UsageEvents.Event()
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    frente?.let { somar(it, desde, e.timeStamp) }
                    frente = e.packageName
                    desde = e.timeStamp
                    if (e.packageName !in ignorar) {
                        sessaoApps += e.packageName
                        if (sessaoInicio == 0L) sessaoInicio = e.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> if (e.packageName == frente) {
                    somar(e.packageName, desde, e.timeStamp)
                    frente = null
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    desbloqueios++
                    if (primeiro == null) primeiro = e.timeStamp
                    if (sessaoInicio == 0L) sessaoInicio = e.timeStamp
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    frente?.let { somar(it, desde, e.timeStamp) }
                    frente = null
                    fecharSessao(e.timeStamp)
                }
            }
        }
        frente?.let { somar(it, desde, fim) }
        fecharSessao(fim)

        // juntar sessoes com menos de 3 minutos de intervalo (olhou de novo logo depois)
        val juntas = mutableListOf<Sessao>()
        for (s in sessoes) {
            val ultima = juntas.lastOrNull()
            if (ultima != null && s.inicio - ultima.fim < 3 * 60_000) {
                juntas[juntas.lastIndex] = Sessao(ultima.inicio, s.fim, (ultima.pacotes + s.pacotes).distinct())
            } else {
                juntas += s
            }
        }
        return ResumoDia(data, porApp.values.sum(), desbloqueios, primeiro, juntas, porApp, porHora)
    }

    /** Os ultimos [dias] dias, do mais antigo a hoje. */
    fun semana(contexto: Context, dias: Int = 7): List<ResumoDia> {
        val hoje = LocalDate.now()
        return (dias - 1 downTo 0).mapNotNull { dia(contexto, hoje.minusDays(it.toLong())) }
    }

    /** "2h 14m", "38m". */
    fun duracao(ms: Long): String {
        val m = ms / 60_000
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
    }
}
