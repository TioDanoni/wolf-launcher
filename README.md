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
  Enter abre); também busca na web e abre os ajustes.
- **Gestos:** para cima, todos os apps (por letra, com a barra de letras); para baixo, a
  cortina de notificações; segurar num espaço vazio, o menu do launcher.
- **Atualização sozinha:** ver abaixo.

## Próximos passos

1. Desenhar uma letra em qualquer lugar (leque de apps) e o comando completo (pessoas,
   contas, "timer 5 min", ajustes do Android).
2. A Matilha (as pessoas) e as prévias das notificações nos verbos.
3. Seu dia (linha do tempo), verbos que mudam com a hora e o modo noite.

## Atualizações

Cada tag `v*` no GitHub vira uma Release com o APK assinado (`.github/workflows/apk.yml`).
O launcher consulta a última Release a cada 12 h (e ao voltar para a tela, se faz mais de
6 h), avisa na tela inicial e instala por cima — favoritos e ajustes continuam. Na
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
  atualizacao/             consultar o GitHub, baixar, instalar e o trabalho periódico
  ui/Inicio.kt             relógio, faixa do agora, verbos e barra de comando
  ui/Comando.kt            a barra de comando aberta
  ui/VerboTela.kt          os apps de um verbo
  ui/Gaveta.kt             todos os apps por letra
  ui/AjustesTela.kt        ajustes e atualizações
  ui/Comum.kt, Tema.kt     peças repetidas, cores do WolfOS e a aurora
design/                    scripts que geram os mockups (python3 design/gerar_final.py)
```

Código com nomes em português sem acento; o que aparece na tela, com acento (o mesmo
estilo do projeto WolfOS).
