plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("io.freefair.lombok") version "8.0.1"
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "4.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

javafx {
    version = "21.0.7"
    modules("javafx.controls", "javafx.fxml")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.skadistats:clarity:4.0.0")
    implementation("com.tobiasdiez:easybind:2.2")
    implementation("ch.qos.logback:logback-classic:1.5.20")
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
