plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.manekdm13.jarvis"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.manekdm13.jarvis"
        minSdk = 26
        targetSdk = 36
        val versionParts = providers.gradleProperty("jarvisVersionName")
            .orElse("0.3.0").get().split(".")
        versionCode = (versionParts.getOrElse(0) { "0" }.toIntOrNull() ?: 0) * 10000 +
            (versionParts.getOrElse(1) { "0" }.toIntOrNull() ?: 0) * 100 +
            (versionParts.getOrElse(2) { "0" }.toIntOrNull() ?: 0)
        versionName = versionParts.joinToString(".")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("JARVIS_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("JARVIS_STORE_PASSWORD")
                keyAlias = System.getenv("JARVIS_KEY_ALIAS")
                keyPassword = System.getenv("JARVIS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (!System.getenv("JARVIS_STORE_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
