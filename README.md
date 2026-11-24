# WolfLauncher

Launcher (tela inicial) para Android com a cara do WolfOS: minimalista, em texto, com o
fundo escuro e a aurora da cor de destaque — a mesma do Hello no PC. No Android 12 ou mais
novo a cor vem do wallpaper, como o Noctalia faz no desktop.

Kotlin + Jetpack Compose. Pacote `org.wolfos.launcher`, Android 8.0 (API 26) ou mais novo.

## O que já tem (0.1)

- **Tela inicial:** relógio, data e os apps favoritos em texto grande. Deslizar para cima
  abre a gaveta. Voltar não sai do launcher; Home volta sempre para o início.
- **Gaveta:** todos os apps em ordem alfabética (português, acentos ignorados) com a letra
  de cada seção; a barra de letras na direita pula direto (tocar ou arrastar).
- **Busca:** filtra enquanto digita (os que começam com o termo primeiro); Enter abre o
  primeiro.
- **Segurar um app:** pôr/tirar da tela inicial, subir/descer (na tela inicial),
  informações do app e desinstalar.
- A lista acompanha sozinha quando um app é instalado, atualizado ou removido.

## Próximos passos

- Notificações junto do app na lista (serviço `NotificationListenerService`, com permissão).
- Busca também em contatos e nas configurações.
- Gestos: tocar duas vezes para bloquear (serviço de acessibilidade), deslizar para o lado.
- Pacotes de ícones e widgets.
- Ajustes: tamanho da letra, mostrar ícones na tela inicial, esconder apps.

## Compilar

Precisa do JDK 17 e do Android SDK (Android Studio, ou só as ferramentas de linha de
comando com `platforms;android-35` e `build-tools;35.0.0`).

- **Android Studio:** abrir esta pasta; ele baixa o Gradle e gera o `gradlew`.
- **Linha de comando:** com o Gradle instalado, `gradle wrapper` uma vez (gera o
  `gradlew`); depois `./gradlew assembleDebug`. O APK sai em
  `app/build/outputs/apk/debug/`.

Instalar no celular pelo cabo (depuração USB ligada): `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
Depois, em Configurações > Apps > App padrão > Tela inicial, escolher o WolfLauncher.

## Organização

```
app/src/main/java/org/wolfos/launcher/
  MainActivity.kt        a tela (inicio ou gaveta), Home e voltar
  LauncherViewModel.kt   a lista de apps (lida fora da tela) e os favoritos
  dados/Apps.kt          ler, abrir, informações e desinstalar apps; ordem e letras
  dados/Favoritos.kt     os favoritos, na ordem escolhida
  ui/Tema.kt             cores do WolfOS, cor do wallpaper e a aurora
  ui/Inicio.kt           relógio, data e favoritos
  ui/Gaveta.kt           busca, lista por letra e a barra de letras
  ui/MenuApp.kt          o menu de segurar um app
```

Código com nomes em português sem acento; o que aparece na tela, com acento (o mesmo
estilo do projeto WolfOS).
