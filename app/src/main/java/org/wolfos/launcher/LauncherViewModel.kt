package org.wolfos.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Favoritos

/** Estado do launcher: a lista de apps (lida fora da tela) e os favoritos. */
class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val favoritosArq = Favoritos(app)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _favoritos = MutableStateFlow(favoritosArq.ler())
    val favoritos: StateFlow<List<String>> = _favoritos.asStateFlow()

    // instalou, removeu ou atualizou um app: a lista acompanha
    private val pacotes = object : BroadcastReceiver() {
        override fun onReceive(contexto: Context, intent: Intent) = recarregar()
    }

    init {
        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // as mensagens de pacote sao do sistema: chegam mesmo com o receptor fechado para os outros apps
        ContextCompat.registerReceiver(app, pacotes, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
        recarregar()
    }

    fun recarregar() {
        viewModelScope.launch {
            val lista = withContext(Dispatchers.IO) { Apps.carregar(getApplication()) }
            _apps.value = lista
            // favorito de app desinstalado sai sozinho
            val existentes = lista.map { it.chave }.toSet()
            val limpos = _favoritos.value.filter { it in existentes }
            if (lista.isNotEmpty() && limpos != _favoritos.value) definirFavoritos(limpos)
        }
    }

    fun ehFavorito(app: AppInfo) = app.chave in _favoritos.value

    fun alternarFavorito(app: AppInfo) {
        val atual = _favoritos.value
        definirFavoritos(if (app.chave in atual) atual - app.chave else atual + app.chave)
    }

    fun moverFavorito(app: AppInfo, passo: Int) {
        val lista = _favoritos.value.toMutableList()
        val i = lista.indexOf(app.chave)
        val j = i + passo
        if (i < 0 || j !in lista.indices) return
        lista[i] = lista[j].also { lista[j] = lista[i] }
        definirFavoritos(lista)
    }

    private fun definirFavoritos(lista: List<String>) {
        _favoritos.value = lista
        favoritosArq.gravar(lista)
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(pacotes)
    }
}
