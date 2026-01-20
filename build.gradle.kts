plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("io.freefair.lombok") version "8.0.1"
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "3.1-SNAPSHOT"

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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).charSet = "UTF-8"
    (options as StandardJavadocDocletOptions).locale = "en"
}

dependencies {
    implementation("com.skadistats:clarity:3.1.3")
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
