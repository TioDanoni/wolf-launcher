package org.wolfos.launcher.dados

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Um atalho que o proprio app oferece ("Nova conversa", "Tirar selfie"...). */
class AtalhoApp(val rotulo: String, val icone: ImageBitmap?, val info: ShortcutInfo)

/** Os atalhos dos apps (so o launcher padrao pode ler e abrir). */
object Atalhos {
    fun doApp(contexto: Context, app: AppInfo, quantos: Int = 5): List<AtalhoApp> {
        val la = contexto.getSystemService(LauncherApps::class.java) ?: return emptyList()
        if (runCatching { la.hasShortcutHostPermission() }.getOrDefault(false).not()) return emptyList()
        val consulta = LauncherApps.ShortcutQuery()
            .setPackage(app.pacote)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val densidade = contexto.resources.displayMetrics.densityDpi
        return runCatching { la.getShortcuts(consulta, Process.myUserHandle()) }.getOrNull().orEmpty()
            .filter { it.isEnabled && (it.activity == null || it.activity?.className == app.atividade || it.isDynamic) }
            // os fixos do app primeiro, depois os dinamicos (conversas recentes...), cada um pela ordem do app
            .sortedWith(compareBy<ShortcutInfo> { if (it.isDeclaredInManifest) 0 else 1 }.thenBy { it.rank })
            .take(quantos)
            .map { s ->
                AtalhoApp(
                    rotulo = (s.shortLabel ?: s.longLabel)?.toString().orEmpty(),
                    icone = runCatching { la.getShortcutIconDrawable(s, densidade)?.toBitmap(72, 72)?.asImageBitmap() }.getOrNull(),
                    info = s,
                )
            }
    }

    fun abrir(contexto: Context, atalho: AtalhoApp): Boolean = runCatching {
        contexto.getSystemService(LauncherApps::class.java)?.startShortcut(atalho.info, null, null)
    }.isSuccess
}
