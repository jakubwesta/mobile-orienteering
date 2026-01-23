import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.mobileorienteering"
    //noinspection GradleDependency
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    defaultConfig {
        applicationId = "com.mobileorienteering"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1 // Handled via workflow 1.23.45 -> '12345'
        versionName = "0.1.0" // Handled via workflow and TAG
        testInstrumentationRunner = "com.mobileorienteering.HiltTestRunner"

        val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = localProperties.getProperty("KEYSTORE_FILE")
            val keystorePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
            val keyAlias = localProperties.getProperty("KEY_ALIAS")
            val keyPassword = localProperties.getProperty("KEY_PASSWORD")

            if (keystoreFile != null && keystorePassword != null &&
                keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            val releaseUrl =
                localProperties.getProperty("RELEASE_BASE_URL", "http://10.0.2.2:8080/")
            buildConfigField("String", "BASE_URL", "\"$releaseUrl\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

hilt {
    enableAggregatingTask = false
}

dependencies {

    // Shared KMP module
    implementation(project(":shared"))

    // Core & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.splashscreen)

    // Google fonts
    implementation(libs.google.fonts)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt (KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // MapLibre
    implementation(libs.maplibre.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Media3 (ExoPlayer) for video playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Google Play Services
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)

    // Testing - Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.kotlin.test)
    kspTest(libs.hilt.compiler)

    // Testing - Instrumented Tests (UI Tests)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Flow testing (highly recommended)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.turbine)

    // Debug - Required for Compose UI testing
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
