import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Properties

/**
 * Centralized configuration manager for Android app build settings.
 *
 * This object provides type-safe access to build configuration properties from multiple sources:
 * - Environment variables (highest priority)
 * - local.properties file
 * - gradle.properties file (lowest priority)
 *
 * Properties are resolved in order of precedence, with environment variables taking priority
 * over property files. This allows for flexible configuration across different environments
 * (local development, CI/CD, production).
 *
 * Must be initialized with init() before accessing any properties.
 */
object AppConfig {
	// Lazy-initialized properties to hold configuration sources
	private lateinit var localProperties: Properties
	private lateinit var project: Project

	/**
	 * Initializes the configuration manager with property sources.
	 *
	 * Must be called before accessing any configuration properties.
	 *
	 * @param localProps Properties loaded from local.properties file
	 * @param proj The Gradle project instance
	 */
	fun init(localProps: Properties, proj: Project) {
		localProperties = localProps
		project = proj
	}

	/**
	 * Retrieves a property value from multiple sources with fallback support.
	 *
	 * Search order:
	 * 1. Environment variable (key converted to SCREAMING_SNAKE_CASE)
	 * 2. local.properties file
	 * 3. gradle.properties file
	 * 4. Default value (if provided)
	 *
	 * @param keys One or more property keys to search for (allows aliases)
	 * @param default Optional default value if property is not found
	 * @return The property value from the first matching source
	 * @throws GradleException if property is not found and no default is provided
	 */
	private fun getProperty(vararg keys: String, default: String? = null): String {
		for (key in keys) {
			// Check environment variable (e.g., "app.compileSdk" -> "APP_COMPILE_SDK")
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

	// Android SDK version configuration

	/** Target Android SDK version for compilation (e.g., 34) */
	val compileSdk: Int get() = getProperty("app.compileSdk").toInt()

	/** Minimum Android SDK version supported by the app (e.g., 26) */
	val minSdk: Int get() = getProperty("app.minSdk").toInt()

	/** Target Android SDK version for runtime behavior (e.g., 34) */
	val targetSdk: Int get() = getProperty("app.targetSdk").toInt()

	// API endpoint configuration

	/**
	 * Base URL for the backend API in release builds.
	 *
	 * Can be set via RELEASE_BASE_URL environment variable or app.release.baseUrl property.
	 * Example: "https://api.production.com"
	 */
	val releaseBaseUrl: String get() = getProperty(
		"RELEASE_BASE_URL",
		"app.release.baseUrl"
	)

	/**
	 * Base URL for the backend API in debug builds.
	 *
	 * Can be set via DEBUG_BASE_URL environment variable or app.debug.baseUrl property.
	 * Example: "http://localhost:8000" or "https://api.staging.com"
	 */
	val debugBaseUrl: String get() = getProperty(
		"DEBUG_BASE_URL",
		"app.debug.baseUrl"
	)

	// Third-party service configuration

	/**
	 * Google OAuth 2.0 Web Client ID for authentication.
	 *
	 * Required for Google Sign-In functionality. Defaults to empty string if not provided.
	 * Can be set via GOOGLE_WEB_CLIENT_ID environment variable.
	 */
	val googleWebClientId: String get() = getProperty(
		"GOOGLE_WEB_CLIENT_ID",
		default = ""
	)

	// Feature flags

	/**
	 * Controls whether debug logging is enabled in the app.
	 *
	 * Defaults to true. Set to false in production to reduce log noise and improve performance.
	 * Can be set via app.feature.loggingEnabled property.
	 */
	val loggingEnabled: Boolean get() = getProperty(
		"app.feature.loggingEnabled",
		default = "true"
	).toBoolean()
}
