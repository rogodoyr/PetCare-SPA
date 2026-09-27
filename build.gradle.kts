plugins {
    kotlin("jvm") version "2.4.10"
    application
}

group = "org.example"

application {
    mainClass.set("org.example.MainKt")
}
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Corrutinas de Kotlin: necesarias para las operaciones asíncronas de entrada/salida
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("file.encoding", "UTF-8")
    standardInput = System.`in`
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}
