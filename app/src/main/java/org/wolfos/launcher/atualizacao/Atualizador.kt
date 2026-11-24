package org.wolfos.launcher.atualizacao

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.wolfos.launcher.BuildConfig
import org.wolfos.launcher.dados.Preferencias
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Uma versao publicada no GitHub. */
data class Versao(val nome: String, val notas: String, val apk: String)

sealed interface EstadoAtualizacao {
    data object EmDia : EstadoAtualizacao
    data class Disponivel(val versao: Versao) : EstadoAtualizacao
    /** O Android ainda nao deixou o launcher instalar apps (primeira vez). */
    data class PrecisaPermissao(val versao: Versao) : EstadoAtualizacao
    data class Baixando(val versao: Versao, val progresso: Float) : EstadoAtualizacao
    data class Instalando(val versao: Versao) : EstadoAtualizacao
    /** O Android pediu para a pessoa confirmar (tocar em "Concluir"). */
    data class Confirmar(val versao: Versao, val intent: Intent) : EstadoAtualizacao
    data class Erro(val versao: Versao?, val mensagem: String) : EstadoAtualizacao
}

/**
 * Atualizacao sozinha pelo GitHub: cada tag v* vira uma Release com o APK assinado
 * (ver .github/workflows/apk.yml). O launcher consulta a ultima, baixa e instala por
 * cima. Do Android 12 em diante, depois da primeira vez, nem pede confirmacao.
 */
object Atualizador {
    private const val REPOSITORIO = "TioDanoni/wolf-launcher"
    private const val API = "https://api.github.com/repos/$REPOSITORIO/releases/latest"

    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.EmDia)
    val estado: StateFlow<EstadoAtualizacao> = _estado.asStateFlow()

    private val trava = Mutex()

    val versaoAtual: String get() = BuildConfig.VERSION_NAME

    /** Consulta o GitHub. Devolve a versao nova, se tiver. */
    suspend fun verificar(contexto: Context): Versao? = withContext(Dispatchers.IO) {
        // sem internet ou GitHub fora: tenta de novo na proxima vez, sem incomodar
        val json = runCatching { baixarTexto(API) }.getOrNull() ?: return@withContext null
        Preferencias(contexto).ultimaVerificacao = System.currentTimeMillis()
        val rel = JSONObject(json)
        val tag = rel.optString("tag_name").removePrefix("v")
        val assets = rel.optJSONArray("assets")
        val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?.optString("browser_download_url")
        if (apk.isNullOrEmpty() || !maisNova(tag, versaoAtual)) return@withContext null
        val versao = Versao(tag, rel.optString("body").trim(), apk)
        // nao atrapalha uma atualizacao que ja esta andando
        val atual = _estado.value
        if (atual is EstadoAtualizacao.EmDia || atual is EstadoAtualizacao.Erro || atual is EstadoAtualizacao.Disponivel) {
            _estado.value = EstadoAtualizacao.Disponivel(versao)
        }
        versao
    }

    fun podeInstalar(contexto: Context) = contexto.packageManager.canRequestPackageInstalls()

    /** Abre a tela do Android "permitir instalar apps desta fonte". */
    fun pedirPermissao(contexto: Context) {
        runCatching {
            contexto.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${contexto.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Baixa o APK e entrega para o instalador do Android. */
    suspend fun baixarEInstalar(contexto: Context, versao: Versao) {
        if (!trava.tryLock()) return
        try {
            if (!podeInstalar(contexto)) {
                _estado.value = EstadoAtualizacao.PrecisaPermissao(versao)
                return
            }
            val apk = withContext(Dispatchers.IO) { baixarApk(contexto, versao) }
            _estado.value = EstadoAtualizacao.Instalando(versao)
            withContext(Dispatchers.IO) { instalar(contexto, apk) }
        } catch (e: Exception) {
            _estado.value = EstadoAtualizacao.Erro(versao, e.message ?: "não deu para baixar")
        } finally {
            trava.unlock()
        }
    }

    /** Chamado pelo [ReceptorInstalacao] com a resposta do Android. */
    internal fun resultado(contexto: Context, status: Int, mensagem: String?, confirmar: Intent?) {
        val versao = versaoDoEstado()
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> if (confirmar != null && versao != null) {
                val intent = confirmar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                _estado.value = EstadoAtualizacao.Confirmar(versao, intent)
                // com o launcher na tela abre direto; com outro app aberto, fica o aviso para tocar
                runCatching { contexto.startActivity(intent) }
            }
            PackageInstaller.STATUS_SUCCESS -> _estado.value = EstadoAtualizacao.EmDia
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                _estado.value = versao?.let { EstadoAtualizacao.Disponivel(it) } ?: EstadoAtualizacao.EmDia
            else -> _estado.value = EstadoAtualizacao.Erro(versao, mensagem ?: "a instalação falhou")
        }
    }

    /** O launcher voltou para a tela: se a permissao foi dada, segue a atualizacao. */
    suspend fun aoVoltar(contexto: Context) {
        val e = _estado.value
        if (e is EstadoAtualizacao.PrecisaPermissao && podeInstalar(contexto)) baixarEInstalar(contexto, e.versao)
    }

    private fun versaoDoEstado(): Versao? = when (val e = _estado.value) {
        is EstadoAtualizacao.Disponivel -> e.versao
        is EstadoAtualizacao.PrecisaPermissao -> e.versao
        is EstadoAtualizacao.Baixando -> e.versao
        is EstadoAtualizacao.Instalando -> e.versao
        is EstadoAtualizacao.Confirmar -> e.versao
        is EstadoAtualizacao.Erro -> e.versao
        EstadoAtualizacao.EmDia -> null
    }

    private fun baixarApk(contexto: Context, versao: Versao): File {
        val destino = File(contexto.cacheDir, "atualizacao.apk")
        _estado.value = EstadoAtualizacao.Baixando(versao, 0f)
        val conexao = abrir(versao.apk)
        try {
            val total = conexao.contentLengthLong
            conexao.inputStream.use { entrada ->
                destino.outputStream().use { saida ->
                    val buffer = ByteArray(64 * 1024)
                    var lidos = 0L
                    var ultimo = 0f
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        saida.write(buffer, 0, n)
                        lidos += n
                        if (total > 0) {
                            val p = lidos.toFloat() / total
                            if (p - ultimo >= 0.02f) {
                                ultimo = p
                                _estado.value = EstadoAtualizacao.Baixando(versao, p)
                            }
                        }
                    }
                }
            }
        } finally {
            conexao.disconnect()
        }
        return destino
    }

    private fun instalar(contexto: Context, apk: File) {
        val instalador = contexto.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(contexto.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // atualizar a si mesmo sem perguntar (Android 12+); se nao der, o Android pede confirmacao
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = instalador.createSession(params)
        instalador.openSession(id).use { sessao ->
            sessao.openWrite("wolflauncher.apk", 0, apk.length()).use { saida ->
                apk.inputStream().use { it.copyTo(saida) }
                sessao.fsync(saida)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val aviso = PendingIntent.getBroadcast(
                contexto, id, Intent(contexto, ReceptorInstalacao::class.java), flags,
            )
            sessao.commit(aviso.intentSender)
        }
    }

    private fun abrir(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "WolfLauncher/$versaoAtual")
            if (responseCode !in 200..299) {
                disconnect()
                throw IllegalStateException("GitHub respondeu $responseCode")
            }
        }

    private fun baixarTexto(url: String): String {
        val conexao = abrir(url)
        return try {
            conexao.inputStream.bufferedReader().readText()
        } finally {
            conexao.disconnect()
        }
    }

    /** "0.10.0" e mais nova que "0.9.3" (compara numero a numero, nao como texto). */
    fun maisNova(candidata: String, atual: String): Boolean {
        fun partes(v: String) = v.split('.', '-').map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val a = partes(candidata)
        val b = partes(atual)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
