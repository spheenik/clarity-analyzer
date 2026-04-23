plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("io.freefair.lombok") version "8.14.4"
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "5.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
    implementation("com.skadistats:clarity:5.0.0-SNAPSHOT")
    implementation("com.tobiasdiez:easybind:2.2") {
        // easybind declares javafx-base:14 (ancient). It's conflict-upgraded
        // to 21.0.7, but arrives through a second dependency path — the same
        // artifact is then referenced twice on runtimeClasspath, tripping
        // packageUnoJar's "duplicate entry" bug. Drop the transitive here;
        // javafx-controls → javafx-graphics already pulls a newer javafx-base.
        exclude(group = "org.openjfx", module = "javafx-base")
    }
    implementation("ch.qos.logback:logback-classic:1.5.32")
    // javafxplugin already adds the host-platform classifier of
    // javafx-graphics to runtimeClasspath. Adding it a second time here
    // collides with it during packageUnoJar ("duplicate entry" ZipException).
    // Declare only the foreign-platform classifiers so the uno-jar stays
    // cross-platform without double-declaring the host's.
    val currentOs = org.gradle.internal.os.OperatingSystem.current()
    listOf("win", "linux", "mac").filterNot { p ->
        (p == "linux" && currentOs.isLinux) ||
        (p == "win" && currentOs.isWindows) ||
        (p == "mac" && currentOs.isMacOsX)
    }.forEach { runtimeOnly("org.openjfx:javafx-graphics:${javafx.version}:$it") }
}

application {
    mainClass.set("skadistats.clarity.analyzer.AnalyzerLauncher")
}

unojar {
    archiveVersion.set("")
    archiveClassifier.set("")
}
