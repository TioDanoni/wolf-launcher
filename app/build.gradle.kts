plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// A versao vem da tag no GitHub (v0.2.0 -> -Pversao=0.2.0); sem ela, a de desenvolvimento.
val versao = (findProperty("versao") as String?)?.removePrefix("v") ?: "0.1.0"
val codigoVersao = versao.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    .let { (it.getOrElse(0) { 0 } * 10000) + (it.getOrElse(1) { 0 } * 100) + it.getOrElse(2) { 0 } }

// A chave de assinatura: a mesma em toda versao, senao o Android nao aceita atualizar por cima.
// No GitHub vem dos segredos; no PC, das variaveis WOLF_CHAVE, WOLF_CHAVE_SENHA.
val arquivoChave = System.getenv("WOLF_CHAVE")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "org.wolfos.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.wolfos.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = codigoVersao
        versionName = versao
    }

    signingConfigs {
        if (arquivoChave != null) {
            create("wolf") {
                storeFile = arquivoChave
                storeType = "pkcs12"
                storePassword = System.getenv("WOLF_CHAVE_SENHA")
                keyAlias = "wolflauncher"
                keyPassword = System.getenv("WOLF_CHAVE_SENHA")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("wolf")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime)
    // a cor de cada icone (a aura atras dele no painel)
    implementation(libs.androidx.palette)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    // icones de traco (o R8 tira do APK os que nao sao usados)
    implementation(libs.androidx.compose.icones)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
