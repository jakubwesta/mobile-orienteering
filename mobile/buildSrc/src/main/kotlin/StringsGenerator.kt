import com.squareup.kotlinpoet.*
import org.dom4j.io.SAXReader
import org.dom4j.Element
import java.io.File
import kotlin.reflect.KClass

/**
 * Generates type-safe Kotlin code from Android strings.xml resources.
 *
 * This generator parses strings.xml and creates a Strings object with nested objects
 * for each resource group. It supports:
 * - Simple string resources (as properties with Composable getters)
 * - Formatted strings with parameters (as functions)
 * - Plural resources (as objects with invoke operators)
 *
 * Each resource is available in two variants:
 * 1. Composable function using stringResource() - for use in @Composable functions
 * 2. Context-based function - for use in non-Composable code
 */
class StringsGenerator(
	private val stringsXmlFile: File,
	private val outputDir: File,
	private val packageName: String = "com.mobileorienteering.ui.core"
) {

	// Android and Compose annotations used in generated code
	private val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
	private val readOnlyComposableAnnotation = ClassName("androidx.compose.runtime", "ReadOnlyComposable")
	private val rClass = ClassName("com.mobileorienteering", "R")
	private val contextClass = ClassName("android.content", "Context")

	/**
	 * Main entry point - generates the Strings.kt file from strings.xml.
	 *
	 * Process:
	 * 1. Parse strings.xml to extract all string, formatted, and plural resources
	 * 2. Build a Strings object with nested group objects
	 * 3. Generate Kotlin code using KotlinPoet
	 * 4. Clean up visibility modifiers (remove redundant 'public')
	 */
	fun generate() {
		if (!stringsXmlFile.exists()) {
			throw IllegalStateException("strings.xml not found at ${stringsXmlFile.absolutePath}")
		}

		// Parse XML document
		val reader = SAXReader()
		val document = reader.read(stringsXmlFile)
		val root = document.rootElement

		// Extract different types of string resources
		val stringResources = parseStringResources(root)
		val formattedResources = parseFormattedResources(root)
		val pluralsResources = parsePluralsResources(root)

		// Build the file specification with imports and header comments
		val fileSpec = FileSpec.builder(packageName, "GeneratedStrings")
			.addFileComment("Generated from src/main/res/values/strings.xml; ")
			.addFileComment("Run './gradlew generateStrings' to regenerate")
			.addImport("androidx.compose.ui.res", "stringResource")
			.addImport("androidx.compose.ui.res", "pluralStringResource")
			.indent("\t")
			.build()

		// Build the main Strings object with all nested groups
		val stringsObject = buildStringsObject(stringResources, formattedResources, pluralsResources)
		val finalFileSpec = fileSpec.toBuilder()
			.addType(stringsObject)
			.build()

		// Write to file
		finalFileSpec.writeTo(outputDir)

		// Post-process: remove redundant 'public' visibility modifiers
		val generatedFile = File(outputDir, "${packageName.replace(".", "/")}/GeneratedStrings.kt")
		if (generatedFile.exists()) {
			val content = generatedFile.readText()
			val cleanedContent = content.replace("public object ", "object ")
				.replace("public val ", "val ")
				.replace("public fun ", "fun ")
			generatedFile.writeText(cleanedContent)
		}

		println("Generated Strings.kt")
	}

	/**
	 * Parses simple string resources (without format parameters).
	 *
	 * Groups strings by prefix (e.g., "auth_login" -> group "auth", property "login").
	 * Strings without underscore are placed in the "app" group.
	 *
	 * @return Map of group name to list of string resources in that group
	 */
	private fun parseStringResources(root: Element): Map<String, List<StringResource>> {
		val stringsByGroup = mutableMapOf<String, MutableList<StringResource>>()

		root.elements("string").forEach { element ->
			val name = element.attributeValue("name") ?: return@forEach
			val value = element.text ?: ""

			// Skip formatted strings (those with %1$s, %2$d, etc.) - they're handled separately
			if (value.contains(Regex("%\\d+\\$"))) return@forEach

			// Split name by first underscore to determine group and property name
			val parts = name.split("_", limit = 2)
			val (group, propertyName) = if (parts.size == 2) {
				parts[0] to parts[1].toCamelCase()
			} else {
				"app" to name.toCamelCase()
			}

			stringsByGroup.getOrPut(group) { mutableListOf() }
				.add(StringResource(name, propertyName))
		}

		return stringsByGroup
	}

	/**
	 * Parses formatted string resources (with format parameters like %1$s, %2$d).
	 *
	 * These strings require parameters and are generated as functions rather than properties.
	 *
	 * @return List of formatted resources with their detected parameters
	 */
	private fun parseFormattedResources(root: Element): List<FormattedResource> {
		val formatted = mutableListOf<FormattedResource>()

		root.elements("string").forEach { element ->
			val name = element.attributeValue("name") ?: return@forEach
			val value = element.text ?: ""

			// Only process strings with format parameters
			if (!value.contains(Regex("%\\d+\\$"))) return@forEach

			val params = detectParameters(value)
			formatted.add(
				FormattedResource(
					resourceName = name,
					functionName = name.toCamelCase(),
					parameters = params
				)
			)
		}

		return formatted
	}

	/**
	 * Parses plural string resources (quantity strings).
	 *
	 * Each plural becomes its own object (no grouping by prefix).
	 * Detects if the plural has format parameters by examining the first item.
	 *
	 * @return List of plural resources
	 */
	private fun parsePluralsResources(root: Element): List<PluralsResource> {
		val plurals = mutableListOf<PluralsResource>()

		root.elements("plurals").forEach { element ->
			val name = element.attributeValue("name") ?: return@forEach

			// Check first item to detect format parameters
			val firstItem = element.elements("item").firstOrNull()
			val value = firstItem?.text ?: ""

			val params = if (value.contains(Regex("%\\d+\\$"))) {
				detectParameters(value)
			} else {
				emptyList()
			}

			// Convert full name to PascalCase for the object name
			val objectName = name.toCamelCase().replaceFirstChar { it.uppercase() }

			plurals.add(
				PluralsResource(
					resourceName = name,
					objectName = objectName,
					parameters = params
				)
			)
		}

		return plurals
	}

	/**
	 * Detects format parameters in a string (e.g., %1$s, %2$d, %3$f).
	 *
	 * Maps format specifiers to Kotlin types:
	 * - %d -> Int (named "number" or "number2", "number3", etc.)
	 * - %f -> Float (named "value" or "value2", "value3", etc.)
	 * - %s -> String (named "text" or "text2", "text3", etc.)
	 *
	 * @return List of parameters with generated names and types
	 */
	private fun detectParameters(value: String): List<Parameter> {
		val params = mutableListOf<Parameter>()
		val regex = "%(\\d+)\\$([sdf])".toRegex()

		regex.findAll(value).forEach { match ->
			val position = match.groupValues[1].toInt()
			val type = when (match.groupValues[2]) {
				"d" -> Int::class
				"f" -> Float::class
				"s" -> String::class
				else -> String::class
			}

			// Generate meaningful parameter names based on type
			val paramName = when (type) {
				Int::class -> if (position > 1) "number$position" else "number"
				Float::class -> if (position > 1) "value$position" else "value"
				else -> if (position > 1) "text$position" else "text"
			}

			params.add(Parameter(paramName, type))
		}

		return params.distinctBy { it.name }
	}

	/**
	 * Builds the main Strings object containing all nested group objects.
	 *
	 * Structure:
	 * - Strings.GroupName (for simple strings)
	 * - Strings.Formatted (for formatted strings)
	 * - Strings.Plurals.ResourceName (for plural strings)
	 */
	private fun buildStringsObject(
		stringsByGroup: Map<String, List<StringResource>>,
		formattedResources: List<FormattedResource>,
		pluralsResources: List<PluralsResource>
	): TypeSpec {
		val stringsObject = TypeSpec.objectBuilder("Strings")

		// Add nested objects for each string resource group
		stringsByGroup.forEach { (group, strings) ->
			val groupName = group.replaceFirstChar { it.uppercase() }
			val groupObject = TypeSpec.objectBuilder(groupName)

			strings.forEach { resource ->
				groupObject.addProperty(buildStringProperty(resource))
				groupObject.addFunction(buildStringFunction(resource))
			}

			stringsObject.addType(groupObject.build())
		}

		// Add Formatted object if there are formatted strings
		if (formattedResources.isNotEmpty()) {
			val formattedObject = TypeSpec.objectBuilder("Formatted")

			formattedResources.forEach { resource ->
				formattedObject.addFunction(buildFormattedFunction(resource))
				formattedObject.addFunction(buildFormattedFunctionWithContext(resource))
			}

			stringsObject.addType(formattedObject.build())
		}

		// Add Plurals object with nested objects for each plural resource
		if (pluralsResources.isNotEmpty()) {
			val pluralsObject = TypeSpec.objectBuilder("Plurals")

			pluralsResources.forEach { resource ->
				val pluralObject = TypeSpec.objectBuilder(resource.objectName)
				pluralObject.addFunction(buildPluralsFunction(resource))
				pluralObject.addFunction(buildPluralsFunctionWithContext(resource))
				pluralsObject.addType(pluralObject.build())
			}

			stringsObject.addType(pluralsObject.build())
		}

		return stringsObject.build()
	}

	/**
	 * Builds a property for a simple string resource.
	 *
	 * Generates a property with a @Composable getter that calls stringResource().
	 * Example: val login: String @Composable get() = stringResource(R.string.auth_login)
	 */
	private fun buildStringProperty(resource: StringResource): PropertySpec {
		return PropertySpec.builder(resource.propertyName, String::class)
			.getter(
				FunSpec.getterBuilder()
					.addAnnotation(composableAnnotation)
					.addAnnotation(readOnlyComposableAnnotation)
					.addStatement("return stringResource(%T.string.%L)", rClass, resource.resourceName)
					.build()
			)
			.build()
	}

	/**
	 * Builds a Context-based function for a simple string resource.
	 *
	 * Generates a function that takes Context and calls context.getString().
	 * Example: fun login(context: Context): String = context.getString(R.string.auth_login)
	 */
	private fun buildStringFunction(resource: StringResource): FunSpec {
		return FunSpec.builder(resource.propertyName)
			.addParameter("context", contextClass)
			.returns(String::class)
			.addStatement("return context.getString(%T.string.%L)", rClass, resource.resourceName)
			.build()
	}

	/**
	 * Builds a @Composable function for a formatted string resource.
	 *
	 * Generates a function with parameters matching the format specifiers.
	 * Example: @Composable fun welcome(text: String): String = stringResource(R.string.welcome, text)
	 */
	private fun buildFormattedFunction(resource: FormattedResource): FunSpec {
		val functionBuilder = FunSpec.builder(resource.functionName)
			.addAnnotation(composableAnnotation)
			.addAnnotation(readOnlyComposableAnnotation)
			.returns(String::class)

		resource.parameters.forEach { param ->
			functionBuilder.addParameter(param.name, param.type)
		}

		val paramNames = resource.parameters.joinToString(", ") { it.name }
		functionBuilder.addStatement(
			"return stringResource(%T.string.%L, %L)",
			rClass,
			resource.resourceName,
			paramNames
		)

		return functionBuilder.build()
	}

	/**
	 * Builds a Context-based function for a formatted string resource.
	 *
	 * Similar to buildFormattedFunction but uses Context instead of @Composable.
	 */
	private fun buildFormattedFunctionWithContext(resource: FormattedResource): FunSpec {
		val functionBuilder = FunSpec.builder(resource.functionName)
			.addParameter("context", contextClass)
			.returns(String::class)

		resource.parameters.forEach { param ->
			functionBuilder.addParameter(param.name, param.type)
		}

		val paramNames = resource.parameters.joinToString(", ") { it.name }
		functionBuilder.addStatement(
			"return context.getString(%T.string.%L, %L)",
			rClass,
			resource.resourceName,
			paramNames
		)

		return functionBuilder.build()
	}

	/**
	 * Builds a @Composable invoke operator function for a plural resource.
	 *
	 * Always includes a 'count' parameter, plus any additional format parameters.
	 * The operator function allows calling the object like: Strings.Plurals.CheckpointCount(5)
	 * Example: @Composable operator fun invoke(count: Int): String = pluralStringResource(R.plurals.items, count)
	 */
	private fun buildPluralsFunction(resource: PluralsResource): FunSpec {
		val functionBuilder = FunSpec.builder("invoke")
			.addModifiers(KModifier.OPERATOR)
			.addAnnotation(composableAnnotation)
			.addAnnotation(readOnlyComposableAnnotation)
			.addParameter("count", Int::class)
			.returns(String::class)

		resource.parameters.forEach { param ->
			functionBuilder.addParameter(param.name, param.type)
		}

		if (resource.parameters.isEmpty()) {
			functionBuilder.addStatement(
				"return pluralStringResource(%T.plurals.%L, count)",
				rClass,
				resource.resourceName
			)
		} else {
			val paramNames = listOf("count") + resource.parameters.map { it.name }
			functionBuilder.addStatement(
				"return pluralStringResource(%T.plurals.%L, %L)",
				rClass,
				resource.resourceName,
				paramNames.joinToString(", ")
			)
		}

		return functionBuilder.build()
	}

	/**
	 * Builds a Context-based invoke operator function for a plural resource.
	 *
	 * Similar to buildPluralsFunction but uses Context.resources.getQuantityString().
	 * Example: operator fun invoke(context: Context, count: Int): String
	 */
	private fun buildPluralsFunctionWithContext(resource: PluralsResource): FunSpec {
		val functionBuilder = FunSpec.builder("invoke")
			.addModifiers(KModifier.OPERATOR)
			.addParameter("context", contextClass)
			.addParameter("count", Int::class)
			.returns(String::class)

		resource.parameters.forEach { param ->
			functionBuilder.addParameter(param.name, param.type)
		}

		if (resource.parameters.isEmpty()) {
			functionBuilder.addStatement(
				"return context.resources.getQuantityString(%T.plurals.%L, count, count)",
				rClass,
				resource.resourceName
			)
		} else {
			val paramNames = listOf("count") + resource.parameters.map { it.name }
			functionBuilder.addStatement(
				"return context.resources.getQuantityString(%T.plurals.%L, count, %L)",
				rClass,
				resource.resourceName,
				paramNames.joinToString(", ")
			)
		}

		return functionBuilder.build()
	}

	/**
	 * Converts snake_case to camelCase.
	 *
	 * Example: "auth_login_button" -> "authLoginButton"
	 */
	private fun String.toCamelCase(): String {
		return split("_").mapIndexed { index, part ->
			if (index == 0) part else part.replaceFirstChar { it.uppercase() }
		}.joinToString("")
	}

	// Data classes representing different types of string resources

	/**
	 * Represents a simple string resource without format parameters.
	 *
	 * @property resourceName The name in strings.xml (e.g., "auth_login")
	 * @property propertyName The camelCase property name (e.g., "login")
	 */
	data class StringResource(
		val resourceName: String,
		val propertyName: String
	)

	/**
	 * Represents a formatted string resource with parameters.
	 *
	 * @property resourceName The name in strings.xml
	 * @property functionName The camelCase function name
	 * @property parameters List of detected parameters with names and types
	 */
	data class FormattedResource(
		val resourceName: String,
		val functionName: String,
		val parameters: List<Parameter>
	)

	/**
	 * Represents a plural string resource.
	 *
	 * @property resourceName The name in strings.xml
	 * @property objectName The PascalCase object name
	 * @property parameters List of additional format parameters (beyond count)
	 */
	data class PluralsResource(
		val resourceName: String,
		val objectName: String,
		val parameters: List<Parameter>
	)

	/**
	 * Represents a function parameter with name and type.
	 *
	 * @property name The parameter name (e.g., "text", "number")
	 * @property type The Kotlin type (String::class, Int::class, Float::class)
	 */
	data class Parameter(
		val name: String,
		val type: KClass<*>
	)
}
