plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("io.freefair.lombok") version "8.0.1"
}

group = "com.skadistats"
version = "2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

javafx {
    version = "20"
    modules("javafx.controls", "javafx.fxml")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.skadistats:clarity:3.0.0-SNAPSHOT")
    implementation("com.tobiasdiez:easybind:2.2")
    implementation("ch.qos.logback:logback-classic:1.3.5")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:win")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:linux")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:mac")
}

application {
    mainClass.set("skadistats.clarity.analyzer.AnalyzerLauncher")
}