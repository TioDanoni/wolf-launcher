package org.wolfos.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wolfos.launcher.atualizacao.Atualizador
import org.wolfos.launcher.avisos.Aviso
import org.wolfos.launcher.avisos.Avisos
import org.wolfos.launcher.avisos.Musica
import org.wolfos.launcher.avisos.Tocando
import org.wolfos.launcher.atualizacao.EstadoAtualizacao
import org.wolfos.launcher.atualizacao.TrabalhoAtualizacao
import org.wolfos.launcher.dados.Agenda
import org.wolfos.launcher.dados.AppInfo
import org.wolfos.launcher.dados.Apps
import org.wolfos.launcher.dados.Contato
import org.wolfos.launcher.dados.Contatos
import org.wolfos.launcher.dados.Conversa
import org.wolfos.launcher.dados.Conversas
import org.wolfos.launcher.dados.Fixadas
import org.wolfos.launcher.dados.FonteMatilha
import org.wolfos.launcher.dados.TipoMatilha
import org.wolfos.launcher.dados.UsoConversa
import org.wolfos.launcher.dados.UsoConversas
import org.wolfos.launcher.dados.Favoritos
import org.wolfos.launcher.dados.Icones
import org.wolfos.launcher.dados.Matilha
import org.wolfos.launcher.dados.Cidade
import org.wolfos.launcher.dados.AcaoGesto
import org.wolfos.launcher.dados.Gesto
import org.wolfos.launcher.dados.CoresWallpaper
import org.wolfos.launcher.dados.Aparelho
import org.wolfos.launcher.dados.Aparelhos
import org.wolfos.launcher.dados.ArrumacaoPainel
import org.wolfos.launcher.dados.Nota
import org.wolfos.launcher.dados.Notas
import org.wolfos.launcher.dados.Insights
import org.wolfos.launcher.dados.Padrao
import org.wolfos.launcher.dados.ResumoDia
import org.wolfos.launcher.dados.PartePainel
import org.wolfos.launcher.dados.TimerWolf
import org.wolfos.launcher.dados.PacoteIcones
import org.wolfos.launcher.ui.EstiloRelogio
import org.wolfos.launcher.ui.Paleta
import org.wolfos.launcher.dados.Previsao
import org.wolfos.launcher.dados.Tempo
import org.wolfos.launcher.dados.Personalizacao
import org.wolfos.launcher.dados.WidgetSalvo
import org.wolfos.launcher.dados.Widgets
import org.wolfos.launcher.bloqueio.Bloqueio
import org.wolfos.launcher.dados.Evento
import org.wolfos.launcher.dados.Preferencias
import org.wolfos.launcher.dados.Ritmo
import org.wolfos.launcher.dados.Uso

/** Estado do launcher: os apps (lidos fora da tela), os favoritos e a atualizacao. */
class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val preferencias = Preferencias(app)

    // ---- os apps: como o Android entrega, com os nomes trocados, e sem os escondidos
    private val personal = Personalizacao(app)
    private val _brutos = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _nomes = MutableStateFlow(personal.nomes())
    private val _escondidos = MutableStateFlow(personal.escondidos())
    val escondidos: StateFlow<Set<String>> = _escondidos.asStateFlow()

    /** Todos os apps, ja com os nomes que a pessoa escolheu (inclui os escondidos). */
    val todosApps: StateFlow<List<AppInfo>> = combine(_brutos, _nomes) { brutos, nomes ->
        if (nomes.isEmpty()) brutos
        else Apps.ordenar(brutos.map { a -> nomes[a.chave]?.let { a.copy(nome = it, nomeOriginal = a.nome) } ?: a })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Os apps que aparecem no launcher (sem os escondidos). */
    val apps: StateFlow<List<AppInfo>> = combine(todosApps, _escondidos) { todos, esc ->
        if (esc.isEmpty()) todos else todos.filter { it.chave !in esc }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    fun renomear(app: AppInfo, nome: String?) {
        // voltar ao nome original e o mesmo que tirar o nome escolhido
        personal.renomear(app.chave, nome?.takeIf { it.trim() != app.nomeOriginal })
        _nomes.value = personal.nomes()
    }

    fun esconder(app: AppInfo, esconder: Boolean = true) {
        personal.esconder(app.chave, esconder)
        _escondidos.value = personal.escondidos()
    }


    // ---- os favoritos do painel (vazio: o launcher usa os mais abertos)
    private val favoritosArq = Favoritos(app)
    private val _favoritos = MutableStateFlow(favoritosArq.ler())
    val favoritos: StateFlow<List<String>> = _favoritos.asStateFlow()

    fun alternarFavorito(app: AppInfo) {
        val atual = _favoritos.value
        definirFavoritos(if (app.chave in atual) atual - app.chave else atual + app.chave)
    }

    fun adicionarFavorito(app: AppInfo) {
        if (app.chave !in _favoritos.value) definirFavoritos(_favoritos.value + app.chave)
    }

    private fun definirFavoritos(lista: List<String>) {
        favoritosArq.gravar(lista)
        _favoritos.value = favoritosArq.ler()
    }

    /** O que a pessoa abre e a que horas (so no celular). */
    val uso = Uso(app)

    /** A fileira "Agora" do painel (sugestoes pela hora, mensagens e musica). */
    private val _ritmo = MutableStateFlow(preferencias.sugestoes)
    val ritmo: StateFlow<Boolean> = _ritmo.asStateFlow()

    private val _modoNoite = MutableStateFlow(preferencias.modoNoite)
    val modoNoite: StateFlow<Boolean> = _modoNoite.asStateFlow()

    private val _janelaNoite = MutableStateFlow(preferencias.noiteInicio to preferencias.noiteFim)
    val janelaNoite: StateFlow<Pair<Int, Int>> = _janelaNoite.asStateFlow()

    private val _fone = MutableStateFlow(false)
    val fone: StateFlow<Boolean> = _fone.asStateFlow()

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _temAgenda = MutableStateFlow(Agenda.temPermissao(app))
    val temAgenda: StateFlow<Boolean> = _temAgenda.asStateFlow()

    private val _alarme = MutableStateFlow<Long?>(null)
    val alarme: StateFlow<Long?> = _alarme.asStateFlow()

    /** Conta quantas vezes o uso mudou (para as sugestoes se refazerem). */
    private val _usoMudou = MutableStateFlow(0)
    val usoMudou: StateFlow<Int> = _usoMudou.asStateFlow()

    val musica: StateFlow<Musica?> = Tocando.musica
    val detalheMusica: StateFlow<String> = Tocando.detalhe
    val clima: StateFlow<org.wolfos.launcher.avisos.Clima?> = Avisos.clima

    // ---- widgets do painel e o bloqueio com dois toques
    private val _widgets = MutableStateFlow(Widgets.ler(app))
    val widgets: StateFlow<List<WidgetSalvo>> = _widgets.asStateFlow()

    fun adicionarWidget(id: Int, altura: Int) = definirWidgets(_widgets.value + WidgetSalvo(id, altura))

    fun removerWidget(id: Int) {
        Widgets.apagar(getApplication(), id)
        definirWidgets(_widgets.value.filter { it.id != id })
    }

    fun mudarAlturaWidget(id: Int, delta: Int) =
        definirWidgets(_widgets.value.map { if (it.id == id) it.copy(altura = (it.altura + delta).coerceIn(80, 640)) else it })

    private fun definirWidgets(lista: List<WidgetSalvo>) {
        Widgets.gravar(getApplication(), lista)
        _widgets.value = lista
    }

    private val _doisToques = MutableStateFlow(preferencias.doisToques)
    val doisToques: StateFlow<Boolean> = _doisToques.asStateFlow()

    private val _bloqueioLigado = MutableStateFlow(Bloqueio.ligado(app))
    val bloqueioLigado: StateFlow<Boolean> = _bloqueioLigado.asStateFlow()

    fun definirDoisToques(valor: Boolean) {
        preferencias.doisToques = valor
        _doisToques.value = valor
    }

    private val _mostrarPlayer = MutableStateFlow(preferencias.mostrarPlayer)
    val mostrarPlayer: StateFlow<Boolean> = _mostrarPlayer.asStateFlow()

    fun definirMostrarPlayer(valor: Boolean) {
        preferencias.mostrarPlayer = valor
        _mostrarPlayer.value = valor
    }

    // ---- o topo: clima (Open-Meteo), compromissos e o fundo animado
    private val _tempoLigado = MutableStateFlow(Tempo.ligado(app))
    val tempoLigado: StateFlow<Boolean> = _tempoLigado.asStateFlow()

    private val _previsao = MutableStateFlow(Tempo.guardada(app))
    val previsao: StateFlow<Previsao?> = _previsao.asStateFlow()

    private val _cidade = MutableStateFlow(Tempo.cidade(app))
    val cidade: StateFlow<Cidade?> = _cidade.asStateFlow()

    private val _fahrenheit = MutableStateFlow(Tempo.fahrenheit(app))
    val fahrenheit: StateFlow<Boolean> = _fahrenheit.asStateFlow()

    private val _compromissos = MutableStateFlow(preferencias.compromissos)
    val compromissos: StateFlow<Boolean> = _compromissos.asStateFlow()

    private val _fundoAnimado = MutableStateFlow(preferencias.fundoAnimado)
    val fundoAnimado: StateFlow<Boolean> = _fundoAnimado.asStateFlow()

    /** Busca o clima se faz mais de 30 min (ou agora, com [forcar]). */
    fun atualizarTempo(forcar: Boolean = false) {
        val ctx = getApplication<Application>()
        if (!_tempoLigado.value || _cidade.value == null) return
        val ultima = _previsao.value?.quando ?: 0L
        if (!forcar && System.currentTimeMillis() - ultima < 30 * 60_000L) return
        viewModelScope.launch { Tempo.atualizar(ctx)?.let { _previsao.value = it } }
    }

    fun definirTempoLigado(v: Boolean) {
        Tempo.definirLigado(getApplication(), v)
        _tempoLigado.value = v
        atualizarTempo(forcar = true)
    }

    fun definirCidade(c: Cidade) {
        Tempo.definirCidade(getApplication(), c)
        _cidade.value = c
        _previsao.value = null
        atualizarTempo(forcar = true)
    }

    fun definirFahrenheit(v: Boolean) {
        Tempo.definirFahrenheit(getApplication(), v)
        _fahrenheit.value = v
        atualizarTempo(forcar = true)
    }

    fun definirCompromissos(v: Boolean) {
        preferencias.compromissos = v
        _compromissos.value = v
    }

    fun definirFundoAnimado(v: Boolean) {
        preferencias.fundoAnimado = v
        _fundoAnimado.value = v
    }

    // ---- o estilo dos icones (vale para o launcher inteiro) e os pacotes instalados
    private val _estiloIcones = MutableStateFlow(preferencias.estiloIcones.also { Icones.definirEstilo(it) })
    val estiloIcones: StateFlow<String> = _estiloIcones.asStateFlow()

    private val _pacotesIcones = MutableStateFlow<List<PacoteIcones>>(emptyList())
    val pacotesIcones: StateFlow<List<PacoteIcones>> = _pacotesIcones.asStateFlow()

    fun carregarPacotesIcones() {
        viewModelScope.launch {
            _pacotesIcones.value = withContext(Dispatchers.IO) { Icones.pacotesInstalados(getApplication()) }
            // o pacote escolhido foi desinstalado: volta para os icones dos apps
            val e = _estiloIcones.value
            if (e.startsWith("pack:") && _pacotesIcones.value.none { "pack:" + it.pacote == e }) definirEstiloIcones("ORIGINAL")
        }
    }

    fun definirEstiloIcones(e: String) {
        preferencias.estiloIcones = e
        Icones.definirEstilo(e)
        _estiloIcones.value = e
    }

    // ---- opcoes da tela inicial: Matilha so fixadas, marca, escuro do fundo, gestos
    // ---- o Painel: notas e tarefas, timer, aparelhos Bluetooth e a arrumacao das partes
    private val notasArq = Notas(app)
    private val _notas = MutableStateFlow(notasArq.ler())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()
    fun recarregarNotas() { _notas.value = notasArq.ler() }
    fun adicionarNota(texto: String, tarefa: Boolean = true) { _notas.value = notasArq.adicionar(texto, tarefa) }
    fun alternarNota(id: Long) { _notas.value = notasArq.alternar(id) }
    fun apagarNota(id: Long) { _notas.value = notasArq.apagar(id) }
    fun limparFeitas() { _notas.value = notasArq.limparFeitas() }

    val timer = TimerWolf(app)
    /** Muda quando o timer ou o cronometro mudam (a tela le de novo). */
    private val _timerMudou = MutableStateFlow(0)
    val timerMudou: StateFlow<Int> = _timerMudou.asStateFlow()
    fun comecarTimer(minutos: Int, rotulo: String) { timer.comecar(getApplication(), minutos, rotulo); _timerMudou.value++ }
    fun pararTimer() { timer.parar(getApplication()); _timerMudou.value++ }
    fun alternarCrono() { timer.cronoAlternar(); _timerMudou.value++ }
    fun zerarCrono() { timer.cronoZerar(); _timerMudou.value++ }
    fun timerTalvezMudou() { _timerMudou.value++ }

    private val _aparelhos = MutableStateFlow<List<Aparelho>>(emptyList())
    val aparelhos: StateFlow<List<Aparelho>> = _aparelhos.asStateFlow()
    fun carregarAparelhos() {
        viewModelScope.launch { _aparelhos.value = withContext(Dispatchers.IO) { Aparelhos.conectados(getApplication()) } }
    }

    private val arrumacao = ArrumacaoPainel(app)
    private val _ordemPainel = MutableStateFlow(arrumacao.ordem())
    val ordemPainel: StateFlow<List<PartePainel>> = _ordemPainel.asStateFlow()
    private val _escondidasPainel = MutableStateFlow(arrumacao.escondidas())
    val escondidasPainel: StateFlow<Set<PartePainel>> = _escondidasPainel.asStateFlow()
    fun moverParte(p: PartePainel, passo: Int) { arrumacao.mover(p, passo); _ordemPainel.value = arrumacao.ordem() }
    fun alternarParte(p: PartePainel) { arrumacao.alternar(p); _escondidasPainel.value = arrumacao.escondidas() }

    // ---- Fase 3: rotinas, Modo Caca e chamadas perdidas
    val rotinas = org.wolfos.launcher.dados.Rotinas(app)
    val caca = org.wolfos.launcher.dados.ModoCaca(app)
    private val _rotinaAtiva = MutableStateFlow(rotinas.ativa())
    val rotinaAtiva: StateFlow<org.wolfos.launcher.dados.Rotina?> = _rotinaAtiva.asStateFlow()
    private val _cacaAte = MutableStateFlow(caca.ate)
    val cacaAte: StateFlow<Long> = _cacaAte.asStateFlow()
    /** Muda quando os ajustes das rotinas ou do foco mudam (a tela de ajustes le de novo). */
    private val _rotinasMudou = MutableStateFlow(0)
    val rotinasMudou: StateFlow<Int> = _rotinasMudou.asStateFlow()
    fun recarregarRotina() {
        _rotinaAtiva.value = rotinas.ativa()
        _cacaAte.value = caca.ate
        _rotinasMudou.value++
    }
    fun ligarRotina(r: org.wolfos.launcher.dados.Rotina?) { rotinas.manual = r; recarregarRotina() }
    fun definirRotinaAutomatica(r: org.wolfos.launcher.dados.Rotina, v: Boolean) { rotinas.definirAutomatica(r, v); recarregarRotina() }
    fun definirRotinaDias(r: org.wolfos.launcher.dados.Rotina, d: Set<Int>) { rotinas.definirDias(r, d); recarregarRotina() }
    fun definirRotinaJanela(r: org.wolfos.launcher.dados.Rotina, ini: Int, fim: Int) { rotinas.definirJanela(r, ini, fim); recarregarRotina() }
    fun definirRotinaApps(r: org.wolfos.launcher.dados.Rotina, l: List<String>) { rotinas.definirApps(r, l); recarregarRotina() }
    fun comecarCaca(minutos: Int) { caca.comecar(minutos); recarregarRotina() }
    fun pararCaca() { caca.parar(); recarregarRotina() }
    fun alternarDistracao(app: AppInfo) {
        if (caca.ehDistracao(app)) {
            if (app.chave in caca.extras()) caca.alternarExtra(app.chave) else caca.alternarLiberado(app.chave)
        } else {
            if (app.chave in caca.liberados()) caca.alternarLiberado(app.chave) else caca.alternarExtra(app.chave)
        }
        recarregarRotina()
    }

    private val _perdidas = MutableStateFlow<List<org.wolfos.launcher.dados.Perdida>>(emptyList())
    val perdidas: StateFlow<List<org.wolfos.launcher.dados.Perdida>> = _perdidas.asStateFlow()
    private val _temChamadas = MutableStateFlow(org.wolfos.launcher.dados.Chamadas.temPermissao(app))
    val temChamadas: StateFlow<Boolean> = _temChamadas.asStateFlow()
    fun carregarPerdidas() {
        val contexto = getApplication<Application>()
        _temChamadas.value = org.wolfos.launcher.dados.Chamadas.temPermissao(contexto)
        if (_temChamadas.value) viewModelScope.launch {
            _perdidas.value = withContext(Dispatchers.IO) { org.wolfos.launcher.dados.Chamadas.perdidasHoje(contexto) }
        }
    }
    /** As perdidas ja vistas (tocou para retornar ou dispensou) nao voltam. */
    private val vistasPrefs = app.getSharedPreferences("perdidas", Context.MODE_PRIVATE)
    private val _perdidasVistas = MutableStateFlow(vistasPrefs.getLong("ate", 0L))
    val perdidasVistas: StateFlow<Long> = _perdidasVistas.asStateFlow()
    fun verPerdidas() {
        val agora = System.currentTimeMillis()
        vistasPrefs.edit().putLong("ate", agora).apply()
        _perdidasVistas.value = agora
    }

    private val _modoLista = MutableStateFlow(preferencias.modoLista)
    val modoLista: StateFlow<String> = _modoLista.asStateFlow()
    fun definirModoLista(v: String) { preferencias.modoLista = v; _modoLista.value = v }

    private val _matilhaOrganizar = MutableStateFlow(preferencias.matilhaOrganizar)
    val matilhaOrganizar: StateFlow<Boolean> = _matilhaOrganizar.asStateFlow()
    fun definirMatilhaOrganizar(v: Boolean) { preferencias.matilhaOrganizar = v; _matilhaOrganizar.value = v }

    private val _matilhaSoFixadas = MutableStateFlow(preferencias.matilhaSoFixadas)
    val matilhaSoFixadas: StateFlow<Boolean> = _matilhaSoFixadas.asStateFlow()
    fun definirMatilhaSoFixadas(v: Boolean) { preferencias.matilhaSoFixadas = v; _matilhaSoFixadas.value = v }

    private val _mostrarMarca = MutableStateFlow(preferencias.mostrarMarca)
    val mostrarMarca: StateFlow<Boolean> = _mostrarMarca.asStateFlow()
    fun definirMostrarMarca(v: Boolean) { preferencias.mostrarMarca = v; _mostrarMarca.value = v }

    private val _escuroInicio = MutableStateFlow(preferencias.escuroInicio)
    val escuroInicio: StateFlow<String> = _escuroInicio.asStateFlow()
    fun definirEscuroInicio(v: String) { preferencias.escuroInicio = v; _escuroInicio.value = v }

    private val _gestos = MutableStateFlow(preferencias.gestos())
    val gestos: StateFlow<Map<Gesto, AcaoGesto>> = _gestos.asStateFlow()
    fun definirGesto(g: Gesto, a: AcaoGesto) { preferencias.definirGesto(g, a); _gestos.value = preferencias.gestos() }

    /** As cores do wallpaper (para a paleta "Do wallpaper"); acompanha quando o wallpaper muda. */
    val coresWallpaper: StateFlow<Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>?> =
        CoresWallpaper.cores.also { CoresWallpaper.iniciar(app) }

    private val _paleta = MutableStateFlow(
        run {
            // a 0.21 passa a moldar as cores pelo wallpaper: aplica uma vez
            val p = app.getSharedPreferences("ajustes", android.content.Context.MODE_PRIVATE)
            if (!p.getBoolean("paleta_021", false)) {
                p.edit().putBoolean("paleta_021", true).apply()
                preferencias.paleta = Paleta.WALLPAPER.name
            }
            Paleta.entries.firstOrNull { it.name == preferencias.paleta } ?: Paleta.WALLPAPER
        },
    )
    val paleta: StateFlow<Paleta> = _paleta.asStateFlow()

    private val _estiloRelogio = MutableStateFlow(
        run {
            // o layout novo da tela inicial (0.17) usa o relogio Vertical: aplica uma vez
            val p = app.getSharedPreferences("ajustes", android.content.Context.MODE_PRIVATE)
            if (!p.getBoolean("layout_017", false)) {
                p.edit().putBoolean("layout_017", true).apply()
                preferencias.estiloRelogio = EstiloRelogio.VERTICAL.name
            }
            EstiloRelogio.entries.firstOrNull { it.name == preferencias.estiloRelogio } ?: EstiloRelogio.VERTICAL
        },
    )
    val estiloRelogio: StateFlow<EstiloRelogio> = _estiloRelogio.asStateFlow()

    fun definirEstiloRelogio(e: EstiloRelogio) {
        preferencias.estiloRelogio = e.name
        _estiloRelogio.value = e
    }

    fun definirPaleta(p: Paleta) {
        preferencias.paleta = p.name
        _paleta.value = p
    }

    private val _mostrarMatilha = MutableStateFlow(preferencias.mostrarMatilha)
    val mostrarMatilha: StateFlow<Boolean> = _mostrarMatilha.asStateFlow()

    private val _instalarSozinho = MutableStateFlow(preferencias.instalarSozinho)
    val instalarSozinho: StateFlow<Boolean> = _instalarSozinho.asStateFlow()

    val atualizacao: StateFlow<EstadoAtualizacao> = Atualizador.estado

    private val _contatos = MutableStateFlow<List<Contato>>(emptyList())
    val contatos: StateFlow<List<Contato>> = _contatos.asStateFlow()

    private val _temContatos = MutableStateFlow(Contatos.temPermissao(app))
    val temContatos: StateFlow<Boolean> = _temContatos.asStateFlow()

    /** As notificacoes (so com o acesso permitido). */
    val avisos: StateFlow<List<Aviso>> = Avisos.lista

    private val _avisosPermitidos = MutableStateFlow(Avisos.permitido(app))
    val avisosPermitidos: StateFlow<Boolean> = _avisosPermitidos.asStateFlow()

    // ---- a Matilha pelo WhatsApp: as conversas recentes e as fixadas
    private val fixadasArq = Fixadas(app)

    private val _conversas = MutableStateFlow<List<Conversa>>(emptyList())
    val conversas: StateFlow<List<Conversa>> = _conversas.asStateFlow()

    private val _fixadas = MutableStateFlow(fixadasArq.ler())
    val fixadas: StateFlow<List<String>> = _fixadas.asStateFlow()

    private val _tipoMatilha = MutableStateFlow(fixadasArq.tipo)
    val tipoMatilha: StateFlow<TipoMatilha> = _tipoMatilha.asStateFlow()

    /** O WhatsApp mexeu nos atalhos (conversa nova, mensagem): a Matilha acompanha. */
    private val atalhos = object : LauncherApps.Callback() {
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) {
            if (packageName in Conversas.PACOTES) carregarConversas()
        }
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = Unit
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = Unit
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = Unit
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = Unit
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = Unit
    }

    fun carregarConversas() {
        viewModelScope.launch {
            _conversas.value = withContext(Dispatchers.IO) { Conversas.carregar(getApplication()) }
        }
    }

    // o uso das conversas (abertas pela Matilha, mensagens recebidas): a ordem inteligente
    private val usoConversasArq = UsoConversas(app)
    private val _usoConversas = MutableStateFlow(usoConversasArq.ler())
    val usoConversas: StateFlow<Map<String, UsoConversa>> = _usoConversas.asStateFlow()

    fun registrarConversa(c: Conversa) {
        usoConversasArq.registrarAbertura(c.chave)
        _usoConversas.value = usoConversasArq.ler()
    }

    fun alternarFixada(c: Conversa) {
        val atual = _fixadas.value
        val nova = if (c.chave in atual) atual - c.chave else atual + c.chave
        fixadasArq.gravar(nova)
        _fixadas.value = nova
    }

    private val _fonteMatilha = MutableStateFlow(fixadasArq.fonte ?: Conversas.fontePadrao(app))
    val fonteMatilha: StateFlow<FonteMatilha> = _fonteMatilha.asStateFlow()

    /** As opcoes de fonte deste celular (os WhatsApps instalados e a agenda). */
    val fontesMatilha: List<FonteMatilha> = Conversas.fontesPossiveis(app)

    fun definirFonteMatilha(f: FonteMatilha) {
        fixadasArq.fonte = f
        _fonteMatilha.value = f
    }

    fun definirTipoMatilha(tipo: TipoMatilha) {
        fixadasArq.tipo = tipo
        _tipoMatilha.value = tipo
    }

    private val matilhaArq = Matilha(app)
    private val _matilha = MutableStateFlow(matilhaArq.ler())

    /** Os ids escolhidos para a Matilha (vazio: os favoritos da agenda). */
    val matilha: StateFlow<List<Long>> = _matilha.asStateFlow()

    /** Se o WolfLauncher e a tela inicial do Android (senao, a faixa pede). */
    private val _ehPadrao = MutableStateFlow(Padrao.ehPadrao(app))
    val ehPadrao: StateFlow<Boolean> = _ehPadrao.asStateFlow()

    // instalou, removeu ou atualizou um app: a lista acompanha
    private val pacotes = object : BroadcastReceiver() {
        override fun onReceive(contexto: Context, intent: Intent) {
            intent.data?.schemeSpecificPart?.let(Icones::esquecer)
            recarregar()
        }
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
        carregarContatos()
        runCatching { app.getSystemService(LauncherApps::class.java)?.registerCallback(atalhos) }
        carregarConversas()
        if (Avisos.permitido(app)) Tocando.iniciar(app)
        TrabalhoAtualizacao.agendar(app)
        // mensagem nova chegou (o servico contou): a ordem da Matilha acompanha
        viewModelScope.launch { Avisos.lista.collect { _usoConversas.value = usoConversasArq.ler() } }
        // sem cidade escolhida ainda: usa a da notificacao do clima (ex.: "Piracicaba: 23°")
        viewModelScope.launch {
            Avisos.clima.collect { c ->
                if (c != null && _cidade.value == null) {
                    Tempo.buscarCidades(c.cidade).firstOrNull()?.let(::definirCidade)
                }
            }
        }
        atualizarTempo()
    }

    /** Le a agenda fora da tela (so se a pessoa deixou). */
    fun carregarContatos() {
        val contexto = getApplication<Application>()
        _temContatos.value = Contatos.temPermissao(contexto)
        if (!_temContatos.value) return
        viewModelScope.launch {
            _contatos.value = withContext(Dispatchers.IO) { Contatos.carregar(contexto) }
        }
    }

    fun recarregar() {
        viewModelScope.launch {
            _brutos.value = withContext(Dispatchers.IO) { Apps.carregar(getApplication()) }
        }
    }

    /** O fone, a agenda e o despertador: o que muda o "agora". */
    // ---- Seu dia / Wolf Insights: o resumo de hoje (com o acesso ao uso)
    private val _resumoHoje = MutableStateFlow<ResumoDia?>(null)
    val resumoHoje: StateFlow<ResumoDia?> = _resumoHoje.asStateFlow()
    private val _temUso = MutableStateFlow(Insights.temPermissao(app))
    val temUso: StateFlow<Boolean> = _temUso.asStateFlow()

    fun carregarDia() {
        val contexto = getApplication<Application>()
        _temUso.value = Insights.temPermissao(contexto)
        if (_temUso.value) viewModelScope.launch {
            _resumoHoje.value = withContext(Dispatchers.IO) { Insights.dia(contexto, java.time.LocalDate.now()) }
        }
        _fone.value = Ritmo.foneConectado(contexto)
        _alarme.value = Ritmo.proximoAlarme(contexto)?.first
        _temAgenda.value = Agenda.temPermissao(contexto)
        if (_temAgenda.value) viewModelScope.launch {
            _eventos.value = withContext(Dispatchers.IO) { Agenda.proximos(contexto) }
        }
    }

    fun registrarAbertura(app: AppInfo) {
        uso.registrar(app)
        _usoMudou.value++
    }

    fun definirRitmo(valor: Boolean) {
        preferencias.sugestoes = valor
        _ritmo.value = valor
    }

    fun definirModoNoite(valor: Boolean) {
        preferencias.modoNoite = valor
        _modoNoite.value = valor
    }

    fun definirJanelaNoite(inicio: Int, fim: Int) {
        preferencias.noiteInicio = inicio
        preferencias.noiteFim = fim
        _janelaNoite.value = inicio to fim
    }

    fun adicionarNaMatilha(id: Long) {
        val atual = _matilha.value.ifEmpty { Matilha.pessoas(_contatos.value, emptyList()).map { it.id } }
        if (id in atual || atual.size >= Matilha.MAXIMO) return
        definirMatilha(atual + id)
    }

    fun tirarDaMatilha(id: Long) {
        val atual = _matilha.value.ifEmpty { Matilha.pessoas(_contatos.value, emptyList()).map { it.id } }
        definirMatilha(atual - id)
    }

    private fun definirMatilha(ids: List<Long>) {
        matilhaArq.gravar(ids)
        _matilha.value = ids
    }

    /** O launcher voltou para a tela: procura versao nova se faz tempo que nao procura. */
    fun aoVoltar() {
        _ehPadrao.value = Padrao.ehPadrao(getApplication())
        _avisosPermitidos.value = Avisos.permitido(getApplication())
        _bloqueioLigado.value = Bloqueio.ligado(getApplication())
        if (_avisosPermitidos.value) Tocando.iniciar(getApplication())
        // a agenda pode ter mudado (ou a permissao chegado) enquanto estava fora
        carregarContatos()
        carregarConversas()
        carregarPacotesIcones()
        recarregarNotas()
        recarregarRotina()
        carregarPerdidas()
        carregarAparelhos()
        _timerMudou.value++
        carregarDia()
        atualizarTempo()
        _usoConversas.value = usoConversasArq.ler()
        viewModelScope.launch {
            Atualizador.aoVoltar(getApplication())
            val passou = System.currentTimeMillis() - preferencias.ultimaVerificacao
            if (passou > INTERVALO_VERIFICACAO) Atualizador.verificar(getApplication())
        }
    }

    fun procurarAtualizacao(aoTerminar: (achou: Boolean) -> Unit = {}) {
        viewModelScope.launch { aoTerminar(Atualizador.verificar(getApplication()) != null) }
    }

    /** O botao do aviso: faz o que o momento pede (atualizar, dar permissao, concluir). */
    fun acaoAtualizacao() {
        val contexto = getApplication<Application>()
        when (val e = atualizacao.value) {
            is EstadoAtualizacao.Disponivel -> viewModelScope.launch { Atualizador.baixarEInstalar(contexto, e.versao) }
            is EstadoAtualizacao.Erro -> e.versao?.let { v -> viewModelScope.launch { Atualizador.baixarEInstalar(contexto, v) } }
            is EstadoAtualizacao.PrecisaPermissao -> Atualizador.pedirPermissao(contexto)
            is EstadoAtualizacao.Confirmar ->
                if (!Apps.abrirTela(contexto, e.intent)) viewModelScope.launch { Atualizador.baixarEInstalar(contexto, e.versao) }
            else -> Unit
        }
    }

    fun definirMostrarMatilha(valor: Boolean) {
        preferencias.mostrarMatilha = valor
        _mostrarMatilha.value = valor
    }

    fun definirInstalarSozinho(valor: Boolean) {
        preferencias.instalarSozinho = valor
        _instalarSozinho.value = valor
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(pacotes)
        runCatching { getApplication<Application>().getSystemService(LauncherApps::class.java)?.unregisterCallback(atalhos) }
    }

    private companion object {
        // uma consulta pequena ao GitHub; o limite dele (60 por hora) fica longe
        const val INTERVALO_VERIFICACAO = 20 * 60 * 1000L
    }
}
