plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("io.freefair.lombok") version "8.0.1"
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "3.0-SNAPSHOT"

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
    implementation("com.skadistats:clarity:3.0.6")
    implementation("com.tobiasdiez:easybind:2.2")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:win")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:linux")
    runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:mac")
}

application {
    mainClass.set("skadistats.clarity.analyzer.AnalyzerLauncher")
}

unojar {
    archiveVersion.set("")
    archiveClassifier.set("")
}
