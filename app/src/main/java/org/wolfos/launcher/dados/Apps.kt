package org.wolfos.launcher.dados

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Um app que abre pelo launcher (cada atividade com icone conta como um).
 * O icone nao fica aqui: ele e carregado so quando aparece na tela (ver [Icones]).
 */
@Immutable
data class AppInfo(
    val nome: String,
    val pacote: String,
    val atividade: String,
    /** Categoria que o proprio app declara (ApplicationInfo.CATEGORY_*), ou -1. */
    val categoria: Int,
    /** O nome que o app tem de verdade (diferente de [nome] quando a pessoa renomeou). */
    val nomeOriginal: String = nome,
) {
    /** Chave estavel (pacote/atividade), usada nas escolhas da pessoa. */
    val chave: String = "$pacote/$atividade"

    /** O nome sem acento e em minusculas, calculado uma vez so (a busca usa a cada letra). */
    val busca: String = semAcento(nome)

    /** Letra da secao na gaveta: A-Z sem acento, ou # para o resto. */
    val letra: String = letraDe(nome)
}

/** Texto sem acento e em minusculas, para buscar e agrupar ("Câmera" -> "camera"). */
fun semAcento(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)

fun letraDe(nome: String): String {
    val c = semAcento(nome.trim()).firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

/** Os apps que batem com a busca: os que comecam com o termo primeiro. */
fun filtrar(apps: List<AppInfo>, busca: String): List<AppInfo> {
    val termo = semAcento(busca.trim())
    if (termo.isEmpty()) return apps
    return apps
        .filter { it.busca.contains(termo) }
        .sortedBy { if (it.busca.startsWith(termo)) 0 else 1 }
}

object Apps {
    /** Todos os apps com icone, em ordem alfabetica (pt-BR), menos o proprio launcher. */
    fun carregar(contexto: Context): List<AppInfo> {
        val pm = contexto.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val lista = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return ordenar(lista
            .filter { it.activityInfo.packageName != contexto.packageName }
            .map { r ->
                AppInfo(
                    nome = r.loadLabel(pm).toString(),
                    pacote = r.activityInfo.packageName,
                    atividade = r.activityInfo.name,
                    categoria = r.activityInfo.applicationInfo.category,
                )
            }
            .distinctBy { it.chave })
    }

    /** Ordem alfabetica em pt-BR (sem ligar para acento); o "#" fica no fim, como nas agendas. */
    fun ordenar(apps: List<AppInfo>): List<AppInfo> {
        val colator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }
        return apps.sortedWith { a, b ->
            val (la, lb) = a.letra to b.letra
            when {
                la == "#" && lb != "#" -> 1
                lb == "#" && la != "#" -> -1
                else -> colator.compare(a.nome, b.nome)
            }
        }
    }

    fun abrir(contexto: Context, app: AppInfo): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.pacote, app.atividade))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        contexto.startActivity(intent)
    }.isSuccess

    fun informacoes(contexto: Context, app: AppInfo) {
        abrirTela(contexto, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pacote}")))
    }

    fun desinstalar(contexto: Context, app: AppInfo) {
        abrirTela(contexto, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pacote}")))
    }

    fun buscarNaWeb(contexto: Context, termo: String) {
        if (!abrirTela(contexto, Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", termo))) {
            abrirTela(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(termo))))
        }
    }

    fun papelDeParede(contexto: Context) {
        abrirTela(contexto, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Papel de parede"))
    }

    /** Abre a cortina de notificacoes (API escondida, mas os launchers usam ha anos). */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun abrirNotificacoes(contexto: Context) {
        runCatching {
            val barra = contexto.getSystemService("statusbar")
            barra.javaClass.getMethod("expandNotificationsPanel").invoke(barra)
        }
    }

    fun abrirTela(contexto: Context, intent: Intent): Boolean = runCatching {
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
