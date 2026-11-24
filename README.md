# WolfLauncher

Launcher (tela inicial) para Android com a cara do WolfOS: escuro, com a aurora da cor de
destaque (a mesma do Hello no PC) e pensado para o polegar. No Android 12 ou mais novo a
cor vem do wallpaper, como o Noctalia faz no desktop.

Em vez de uma lista de apps, a tela inicial mostra **verbos** — o que você quer fazer:
Conversar, Trabalhar, Ouvir, Ir, Ver, Criar, Jogar. O desenho completo está em
`design/mockups/final/` (`00-painel.png` mostra todas as telas).

Kotlin + Jetpack Compose. Pacote `org.wolfos.launcher`, Android 8.0 (API 26) ou mais novo.

## O que já tem

- **Tela inicial:** relógio, a faixa do "agora" (por enquanto, o aviso de atualização), os
  verbos e a barra de comando na altura do polegar.
- **Verbos:** os apps entram sozinhos (pelo pacote conhecido ou pela categoria que o app
  declara). Tocar abre os apps do verbo; segurar um app deixa trocar o verbo dele.
- **Barra de comando:** os resultados sobem a partir da barra (o melhor colado nela, o
  Enter faz ele). Entende apps, pessoas da agenda ("ligar mãe", "msg ana"), contas
  ("12 × 3", "15% de 80"), "timer 5 min", "alarme 7:30", "lanterna" e os ajustes do
  Android ("wi-fi", "bluetooth", "brilho"…); também busca na web.
- **Tela inicial do Android:** na primeira vez pede para ser a tela inicial; enquanto não
  for, a faixa do agora mostra "Usar o WolfLauncher como tela inicial".
- **Matilha:** as pessoas no polegar (escolhidas pelo +, ou os favoritos da agenda), com
  anel quando estão esperando resposta; tocar abre ligar, mensagem, WhatsApp e a conversa.
- **Notificações nos verbos** (com permissão): o verbo mostra a última mensagem e quantas
  tem; dentro dele, "quem está esperando", com abrir e dispensar.
- **Seu dia** (deslizar para a direita): o que passou hoje, o agora (com os apps que você
  costuma abrir nessa hora) e o que vem (agenda, com "Entrar" nas reuniões, e despertador).
- **Ritmo do dia:** os verbos mudam de ordem com a hora, com o fone conectado e com o
  costume de cada hora. A faixa do agora mostra o próximo compromisso.
- **Modo noite** (22:30–07:00, ajustável): cor lilás, só Ouvir, Ver e Conversar, e o
  despertador no topo.
- **Menus:** segurar um app abre os atalhos dele, mudar de verbo, renomear, esconder,
  informações e desinstalar; segurar um verbo deixa renomear, escolher os apps e esconder;
  segurar a tela abre o menu do launcher.
- **Responder** mensagens na tela do verbo (pela ação da própria notificação) e marcar
  como lida.
- **Widgets** num painel (deslizar para a esquerda): adicionar com prévia, maior/menor,
  remover.
- **Tocar duas vezes** num espaço vazio bloqueia a tela (serviço de acessibilidade que
  não lê nada da tela).
- **Ajustes** em seções, com páginas para verbos, apps escondidos e gestos, e o status das
  permissões.
- **Gestos:** para cima, todos os apps (por letra, com a barra de letras); para baixo, a
  cortina de notificações; para a direita, seu dia; segurar num espaço vazio, o menu.
- **Atualização sozinha:** ver abaixo.

## Próximos passos

- Pacotes de ícones.

## Atualizações

Cada tag `v*` no GitHub vira uma Release com o APK assinado (`.github/workflows/apk.yml`).
O launcher consulta a última Release a cada 3 h (e ao voltar para a tela, se faz mais de
20 min), avisa na tela inicial e instala por cima — favoritos e ajustes continuam. Na
primeira vez o Android pede para permitir "instalar apps desta fonte"; do Android 12 em
diante, as próximas atualizações nem pedem confirmação.

Publicar uma versão:

```sh
git tag v0.3.0 && git push origin v0.3.0
```

A chave de assinatura fica fora do repositório (em `~/.local/share/wolflauncher/`) e no
GitHub como os segredos `WOLF_CHAVE_B64` e `WOLF_CHAVE_SENHA`. **Guarde uma cópia dela**:
o Android só aceita atualizar por cima um APK assinado com a mesma chave.

## Compilar no PC

Precisa do JDK 17 e do Android SDK (Android Studio, ou só as ferramentas de linha de
comando com `platforms;android-35` e `build-tools;35.0.0`). Abrir a pasta no Android
Studio, ou com o Gradle 8.11: `gradle assembleDebug`. Sem nada disso instalado, o GitHub
compila a cada push e deixa o APK de teste nos artefatos da execução.

## Organização

```
app/src/main/java/org/wolfos/launcher/
  MainActivity.kt          as telas (início, verbo, comando, todos os apps, ajustes), Home e voltar
  LauncherViewModel.kt     os apps, as escolhas de verbo e a atualização
  dados/Apps.kt            ler, abrir, informações e desinstalar apps; busca e ordem
  dados/Icones.kt          ícones carregados só quando aparecem, num cache pequeno
  dados/Verbos.kt          os verbos e em qual cada app entra
  dados/Preferencias.kt    os ajustes do launcher
  dados/Comandos.kt        o que a barra entende: timer, alarme, contas, lanterna, ajustes
  dados/Contatos.kt        a agenda (busca sem acento) e ligar/mensagem
  dados/Padrao.kt          ser a tela inicial do Android
  dados/Matilha.kt         as pessoas da Matilha e quem está esperando
  dados/Ritmo.kt           a ordem dos verbos pela hora, fone e costume; o modo noite
  dados/Uso.kt             o que é aberto e a que horas (só no celular)
  dados/Agenda.kt          os próximos compromissos
  dados/Conversas.kt       a Matilha pelas conversas do WhatsApp (atalhos de conversa)
  dados/Personalizacao.kt  apps renomeados e escondidos, nomes e verbos escondidos
  dados/Atalhos.kt         os atalhos de cada app (menu de segurar)
  avisos/                  as notificações (serviço com a permissão da pessoa)
  atualizacao/             consultar o GitHub, baixar, instalar e o trabalho periódico
  ui/Inicio.kt             relógio, faixa do agora, verbos e barra de comando
  ui/Comando.kt            a barra de comando aberta
  ui/Gestos.kt             os deslizes (cima, baixo, lados) e as pílulas
  ui/VerboTela.kt          quem está esperando e os apps de um verbo
  ui/MatilhaUi.kt          a fileira da Matilha
  ui/DiaTela.kt            seu dia: antes, agora e depois
  ui/Gaveta.kt             todos os apps por letra
  ui/AjustesTela.kt        ajustes (páginas: principal, verbos, escondidos, gestos)
  ui/Folha.kt              a folha dos menus e as linhas com ícone
  ui/MenuApp.kt            segurar um app
  ui/EditarVerbo.kt        segurar um verbo
  ui/WidgetsTela.kt        o painel de widgets
  dados/Widgets.kt         hospedar os widgets (ids, alturas)
  bloqueio/                o serviço de bloquear com dois toques
  ui/Comum.kt, Tema.kt     peças repetidas, cores do WolfOS e a aurora
design/                    scripts que geram os mockups (python3 design/gerar_final.py)
```

Código com nomes em português sem acento; o que aparece na tela, com acento (o mesmo
estilo do projeto WolfOS).
