package org.wolfos.launcher.dados

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** Ser (ou nao) a tela inicial do Android. */
object Padrao {
    fun ehPadrao(contexto: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val papeis = contexto.getSystemService(RoleManager::class.java)
            if (papeis != null && papeis.isRoleAvailable(RoleManager.ROLE_HOME)) return papeis.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val quem = contexto.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return quem?.activityInfo?.packageName == contexto.packageName
    }

    /**
     * O pedido do proprio Android ("Usar o WolfLauncher como app de tela inicial?").
     * Nulo quando nao da para pedir assim (Android 9 ou mais velho): usar [telaDeAjuste].
     */
    fun pedido(contexto: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val papeis = contexto.getSystemService(RoleManager::class.java) ?: return null
        if (!papeis.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        return papeis.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    /** A tela de ajustes do Android onde se escolhe o app de tela inicial. */
    fun telaDeAjuste(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)
}
