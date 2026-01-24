import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import org.gradle.api.GradleException

object AppConfig {
    private lateinit var localProperties: Properties
    private lateinit var project: Project

    fun init(localProps: Properties, proj: Project) {
        localProperties = localProps
        project = proj
    }

    private fun getProperty(vararg keys: String, default: String? = null): String {
        for (key in keys) {
            val value = System.getenv(key.replace(".", "_").uppercase())
                ?: localProperties.getProperty(key)
                ?: project.findProperty(key) as String?
            if (!value.isNullOrEmpty()) return value
        }

        return default ?: throw GradleException(
            "Required property not found. Please set one of: ${keys.joinToString(", ")}\n" +
                    "Add it to gradle.properties or local.properties"
        )
    }

    // Build configuration
    val compileSdk: Int get() = getProperty("app.compileSdk").toInt()
    val minSdk: Int get() = getProperty("app.minSdk").toInt()
    val targetSdk: Int get() = getProperty("app.targetSdk").toInt()

    // API URLs
    val releaseBaseUrl: String get() = getProperty(
        "RELEASE_BASE_URL",
        "app.release.baseUrl"
    )

    val debugBaseUrl: String get() = getProperty(
        "DEBUG_BASE_URL",
        "app.debug.baseUrl"
    )

    // Secrets
    val googleWebClientId: String get() = getProperty(
        "GOOGLE_WEB_CLIENT_ID",
        default = ""
    )

    // Feature flags
    val loggingEnabled: Boolean get() = getProperty(
        "app.feature.loggingEnabled",
        default = "true"
    ).toBoolean()
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    namespace = "com.mobileorienteering"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    AppConfig.init(localProperties, project)

    compileSdk = AppConfig.compileSdk

    defaultConfig {
        applicationId = "com.mobileorienteering"
        minSdk = AppConfig.minSdk
        targetSdk = AppConfig.targetSdk
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.mobileorienteering.HiltTestRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${AppConfig.googleWebClientId}\"")
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
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "BASE_URL", "\"${AppConfig.debugBaseUrl}\"")
            buildConfigField("Boolean", "LOGGING_ENABLED", "${AppConfig.loggingEnabled}")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            buildConfigField("String", "BASE_URL", "\"${AppConfig.releaseBaseUrl}\"")
            buildConfigField("Boolean", "LOGGING_ENABLED", "false")
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
    implementation(libs.androidx.compose.material.icons.extended)
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

    // Media3 (ExoPlayer)
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

    // Flow testing
    testImplementation(libs.turbine)
    androidTestImplementation(libs.turbine)

    // Debug - Compose UI testing
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Fix KSP + BuildConfig timing issue with AGP 9.0 built-in Kotlin
tasks.matching { it.name.startsWith("ksp") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generate") && it.name.contains("BuildConfig") })
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
