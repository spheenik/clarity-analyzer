plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("io.freefair.lombok") version "8.14.4"
    id("com.gradleup.shadow") version "9.6.1"
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
        // easybind declares javafx-base:14 (ancient). Drop the transitive here;
        // javafx-controls → javafx-graphics already pulls a newer javafx-base.
        exclude(group = "org.openjfx", module = "javafx-base")
    }
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("io.airlift:aircompressor:2.0.3")
    // javafxplugin already adds the host-platform classifier of
    // javafx-graphics to runtimeClasspath. Declare only the foreign-platform
    // classifiers so the fat jar stays cross-platform without
    // double-declaring the host's.
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

tasks.shadowJar {
    archiveVersion.set("")
    archiveClassifier.set("")
    manifest.attributes("Multi-Release" to "true")
    filesMatching(listOf("META-INF/services/**", "META-INF/clarity/providers.txt")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    append("META-INF/clarity/providers.txt")
    exclude(
        "module-info.class",
        "META-INF/versions/*/module-info.class",
        "META-INF/INDEX.LIST",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
    )
}
