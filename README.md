# WolfLauncher

Launcher (tela inicial) para Android com a cara do WolfOS: escuro, com a aurora e os
degradês da paleta, e pensado para o polegar. Kotlin + Jetpack Compose, pacote
`org.wolfos.launcher`, Android 8.0 (API 26) ou mais novo. O desenho está em
`design/mockups/final/`.

## O que tem

- **Relógio** em 5 estilos com fontes de impacto (Uivo, Manuscrito, Lua, Neon, Clássico);
  os números deslizam ao virar o minuto.
- **Clima** (Open-Meteo; cidade e °C/°F configuráveis) e os **compromissos de hoje** da
  Google Agenda, com "Entrar" nas reuniões.
- **Painel de apps:** "Agora" (o que faz sentido no momento: o costume da hora, quem tem
  mensagem esperando, o app que está tocando) e "Favoritos" (até 8, fixados pela
  pessoa; sem nenhum, os mais usados). Cada ícone tem uma aura da cor dele e o número
  de notificações.
- **Player** de música quando algo toca (capa, tempo, controles).
- **Matilha:** as conversas recentes do WhatsApp (ou Business, ou os dois, ou a agenda),
  em ordem inteligente (recência, uso, quem está esperando), com fixar.
- **Segurar um app:** as mensagens dele (responder e marcar como lida), os atalhos do
  app, fixar nos favoritos, renomear, esconder, informações e desinstalar.
- **Busca/comando:** apps, pessoas ("ligar mãe"), contas, timer, alarme, lanterna e
  ajustes do Android.
- **Gestos:** para cima, todos os apps (com o alfabeto em onda); para baixo,
  notificações; para a direita, seu dia; para a esquerda, widgets; dois toques bloqueiam.
- **Modo noite**, paletas de cores, fundo animado e **atualização sozinha** pelo GitHub.

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
  MainActivity.kt          as telas (início, comando, todos os apps, dia, widgets, ajustes)
  LauncherViewModel.kt     o estado do launcher
  dados/                   apps, ícones, favoritos e sugestões, uso, agenda, clima,
                           conversas, contatos, comandos, personalização, widgets
  avisos/                  notificações e o que está tocando (serviço com permissão)
  atualizacao/             consultar o GitHub, baixar e instalar
  bloqueio/                o serviço de bloquear com dois toques
  ui/Inicio.kt             a tela inicial
  ui/PainelApps.kt         o painel "Agora" e "Favoritos" com as auras
  ui/Relogios.kt           os estilos de relógio
  ui/MenuApp.kt            segurar um app
  ui/AjustesTela.kt        ajustes (principal, favoritos, escondidos, gestos)
design/                    scripts e imagens do desenho
```

Código com nomes em português sem acento; o que aparece na tela, com acento (o mesmo
estilo do projeto WolfOS).

## Fontes

Os estilos de relógio usam fontes com a licença SIL Open Font License 1.1 (do acervo
do Google Fonts): Bebas Neue (Uivo), Caveat Brush (Manuscrito), DM Serif Display (Lua)
e Righteous (Neon). Ficam em `app/src/main/res/font/`.
