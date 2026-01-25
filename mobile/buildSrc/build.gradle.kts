plugins {
	`kotlin-dsl`
}

repositories {
	mavenCentral()
	google()
}

dependencies {
	implementation(libs.dom4j)
	implementation(libs.kotlinpoet)
}
