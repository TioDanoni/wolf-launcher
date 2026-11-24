package org.wolfos.launcher.dados

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/** Um widget no painel: o id que o Android deu e a altura escolhida (em dp). */
data class WidgetSalvo(val id: Int, val altura: Int)

/**
 * Os widgets do painel (deslizar para a esquerda). O launcher e o "anfitriao": pede um
 * id ao Android, liga o widget a ele (com a permissao da pessoa na primeira vez) e
 * desenha a vista que o app do widget manda.
 */
object Widgets {
    private const val ANFITRIAO = 4242
    private var host: AppWidgetHost? = null

    fun anfitriao(contexto: Context): AppWidgetHost =
        host ?: AppWidgetHost(contexto.applicationContext, ANFITRIAO).also { host = it }

    fun gerente(contexto: Context): AppWidgetManager = AppWidgetManager.getInstance(contexto)

    /** Todos os widgets que os apps oferecem, pelo nome do app e depois do widget. */
    fun disponiveis(contexto: Context): List<AppWidgetProviderInfo> {
        val pm = contexto.packageManager
        return runCatching { gerente(contexto).installedProviders }.getOrDefault(emptyList())
            .sortedWith(
                compareBy<AppWidgetProviderInfo> { nomeDoApp(contexto, it).lowercase() }
                    .thenBy { it.loadLabel(pm).lowercase() }
            )
    }

    fun nomeDoApp(contexto: Context, info: AppWidgetProviderInfo): String = runCatching {
        val pm = contexto.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
    }.getOrDefault(info.provider.packageName)

    fun novoId(contexto: Context): Int = anfitriao(contexto).allocateAppWidgetId()

    /** Liga o widget ao id; falso quando o Android precisa perguntar a pessoa antes. */
    fun ligar(contexto: Context, id: Int, info: AppWidgetProviderInfo): Boolean = runCatching {
        gerente(contexto).bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
    }.getOrDefault(false)

    /** A pergunta do Android: "Permitir que o WolfLauncher crie widgets?". */
    fun pedidoParaLigar(id: Int, info: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)

    fun apagar(contexto: Context, id: Int) {
        runCatching { anfitriao(contexto).deleteAppWidgetId(id) }
    }

    fun info(contexto: Context, id: Int): AppWidgetProviderInfo? = runCatching { gerente(contexto).getAppWidgetInfo(id) }.getOrNull()

    /** A altura inicial: a minima que o widget pede, entre 90 e 420 dp. */
    fun alturaInicial(contexto: Context, info: AppWidgetProviderInfo): Int {
        val dp = info.minHeight / contexto.resources.displayMetrics.density
        return (dp.toInt().coerceIn(90, 420) / 10) * 10
    }

    // ---- o que fica guardado: os ids e as alturas, na ordem

    fun ler(contexto: Context): List<WidgetSalvo> =
        prefs(contexto).getString("lista", "").orEmpty().split(',').mapNotNull { item ->
            val (id, altura) = item.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            WidgetSalvo(id.toIntOrNull() ?: return@mapNotNull null, altura.toIntOrNull() ?: 160)
        }

    fun gravar(contexto: Context, lista: List<WidgetSalvo>) {
        prefs(contexto).edit().putString("lista", lista.joinToString(",") { "${it.id}:${it.altura}" }).apply()
    }

    private fun prefs(contexto: Context) = contexto.getSharedPreferences("widgets", Context.MODE_PRIVATE)
}
