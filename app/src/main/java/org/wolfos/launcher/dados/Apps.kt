package org.wolfos.launcher.dados

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Um app que abre pelo launcher (cada atividade com icone conta como um). */
data class AppInfo(
    val nome: String,
    val pacote: String,
    val atividade: String,
    val icone: ImageBitmap?,
) {
    /** Chave estavel (pacote/atividade), usada nos favoritos. */
    val chave: String get() = "$pacote/$atividade"

    /** Letra da secao na gaveta: A-Z sem acento, ou # para o resto. */
    val letra: String get() = letraDe(nome)
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

object Apps {
    private const val TAMANHO_ICONE = 112

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
        val colator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }
        return lista
            .filter { it.activityInfo.packageName != contexto.packageName }
            .map { r ->
                val icone = runCatching {
                    r.loadIcon(pm).toBitmap(TAMANHO_ICONE, TAMANHO_ICONE).asImageBitmap()
                }.getOrNull()
                AppInfo(
                    nome = r.loadLabel(pm).toString(),
                    pacote = r.activityInfo.packageName,
                    atividade = r.activityInfo.name,
                    icone = icone,
                )
            }
            .distinctBy { it.chave }
            .sortedWith { a, b ->
                // o "#" (numeros, simbolos) fica no fim, como nas agendas
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
        contexto.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pacote}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun desinstalar(contexto: Context, app: AppInfo) {
        contexto.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pacote}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
