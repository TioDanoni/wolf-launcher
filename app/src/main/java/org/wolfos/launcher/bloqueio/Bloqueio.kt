package org.wolfos.launcher.bloqueio

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Bloquear a tela com dois toques. O Android so deixa isso para um servico de
 * acessibilidade (ou um administrador do aparelho); este nao le nada da tela.
 */
class Bloqueio : AccessibilityService() {
    override fun onServiceConnected() {
        instancia = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instancia = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instancia: Bloqueio? = null

        fun ligado(contexto: Context): Boolean {
            val nome = ComponentName(contexto, Bloqueio::class.java).flattenToString()
            val lista = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return lista.split(':').any { it.equals(nome, ignoreCase = true) }
        }

        /** Bloqueia; devolve falso se o servico ainda nao foi ligado. */
        fun bloquear(): Boolean {
            val s = instancia ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        /** A tela do Android para ligar o servico (direto nele, quando da). */
        fun pedirPermissao(contexto: Context) {
            val componente = ComponentName(contexto, Bloqueio::class.java).flattenToString()
            val direto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME, componente)
            } else {
                null
            }
            for (i in listOfNotNull(direto, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) {
                if (runCatching { contexto.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
            }
        }
    }
}
