package org.wolfos.launcher.atualizacao

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.wolfos.launcher.dados.Preferencias
import java.util.concurrent.TimeUnit

/** De tempos em tempos (com internet e bateria boa) procura versao nova e, se puder, instala. */
class TrabalhoAtualizacao(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {
    override suspend fun doWork(): Result {
        val versao = Atualizador.verificar(applicationContext) ?: return Result.success()
        if (Preferencias(applicationContext).instalarSozinho && Atualizador.podeInstalar(applicationContext)) {
            Atualizador.baixarEInstalar(applicationContext, versao)
        }
        return Result.success()
    }

    companion object {
        fun agendar(contexto: Context) {
            val pedido = PeriodicWorkRequestBuilder<TrabalhoAtualizacao>(3, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(contexto)
                .enqueueUniquePeriodicWork("atualizacao", ExistingPeriodicWorkPolicy.UPDATE, pedido)
        }
    }
}
