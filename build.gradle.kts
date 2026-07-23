plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.0"
}

group = "plus.crates"
version = "5.0.0"
description = "Free crates plugin built for Paper."

val pluginProperties = mapOf(
    "version" to version.toString(),
    "description" to description.toString(),
)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    processResources {
        filesMatching("plugin.yml") {
            expand(pluginProperties)
        }
    }
}
