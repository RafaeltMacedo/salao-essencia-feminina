import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Dados da chave de assinatura, num arquivo fora do git (cabeleleiro/keystore.properties).
// Sem esse arquivo o build release sai sem assinatura e não instala.
val arquivoChave = rootProject.file("keystore.properties")
val dadosChave = Properties().apply {
    if (arquivoChave.exists()) arquivoChave.inputStream().use { load(it) }
}
fun dadoChave(nome: String): String = dadosChave.getProperty(nome)
    ?: throw GradleException("Falta '$nome' em cabeleleiro/keystore.properties (ver cabeleleiro/CLAUDE.md, seção Build).")

// Gerar o APK release sem a chave produziria um APK que as clientes não conseguem
// instalar por cima do atual: melhor parar com uma mensagem clara.
gradle.taskGraph.whenReady {
    if (!arquivoChave.exists() && allTasks.any { it.name.contains("Release") }) {
        throw GradleException(
            "Falta cabeleleiro/keystore.properties (e a assinatura.keystore). Copie os dois da " +
            "cópia de segurança antes de gerar o APK release (ver cabeleleiro/CLAUDE.md, seção Build)."
        )
    }
}

android {
    namespace = "com.example.cabeleleiro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cabeleleiro"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (arquivoChave.exists()) {
            create("release") {
                storeFile = rootProject.file(dadoChave("storeFile"))
                storePassword = dadoChave("storePassword")
                keyAlias = dadoChave("keyAlias")
                keyPassword = dadoChave("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (arquivoChave.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.core.ktx) // Adicionamos essa linha de segurança!

    // Necessárias para a lista de agendamentos (RecyclerView) e os cartões (CardView)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Agenda os lembretes locais de agendamento
    implementation("androidx.work:work-runtime:2.9.0")

    // Bibliotecas do Firebase
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-messaging:24.0.0") // avisos push (Cloud Messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}