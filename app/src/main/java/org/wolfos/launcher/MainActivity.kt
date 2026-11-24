package org.wolfos.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.ui.AcoesApp
import org.wolfos.launcher.ui.Aurora
import org.wolfos.launcher.ui.Gaveta
import org.wolfos.launcher.ui.Inicio
import org.wolfos.launcher.ui.WolfTema

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    /** Sobe a cada toque no botao Home: a tela volta para o inicio. */
    private var home by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WolfTema { Tela(vm, home) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        home++
    }
}

@Composable
private fun Tela(vm: LauncherViewModel, home: Int) {
    val contexto = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    var gaveta by rememberSaveable { mutableStateOf(false) }
    var busca by rememberSaveable { mutableStateOf("") }

    // Home: fecha a gaveta e limpa a busca
    LaunchedEffect(home) {
        gaveta = false
        busca = ""
    }
    // voltar fecha a gaveta; na tela inicial nao faz nada (o launcher nao "sai")
    BackHandler(enabled = gaveta) { gaveta = false }
    BackHandler(enabled = !gaveta) {}

    val acoes = AcoesApp(
        abrir = { app ->
            if (Apps.abrir(contexto, app)) {
                gaveta = false
                busca = ""
            }
        },
        ehFavorito = { app -> app.chave in favoritos },
        alternarFavorito = vm::alternarFavorito,
        mover = vm::moverFavorito,
        informacoes = { Apps.informacoes(contexto, it) },
        desinstalar = { Apps.desinstalar(contexto, it) },
    )
    val porChave = apps.associateBy { it.chave }
    // a gaveta escurece mais o wallpaper (a lista fica legivel sobre qualquer fundo)
    val escuro by animateFloatAsState(if (gaveta) 0.88f else 0.35f, label = "escuro")

    Box(Modifier.fillMaxSize()) {
        Aurora(MaterialTheme.colorScheme.primary, escuro)
        AnimatedVisibility(visible = !gaveta, enter = fadeIn(), exit = fadeOut()) {
            Inicio(favoritos.mapNotNull { porChave[it] }, acoes) { gaveta = true }
        }
        AnimatedVisibility(
            visible = gaveta,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            Gaveta(apps, acoes, busca) { busca = it }
        }
    }
}
