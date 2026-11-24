package org.wolfos.launcher.atualizacao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

/** Recebe do Android o resultado da instalacao da versao nova. */
class ReceptorInstalacao : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        Atualizador.resultado(
            contexto,
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            mensagem = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            confirmar = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java),
        )
    }
}
