plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.lombok)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(libs.nexus.core)
    implementation(libs.nexus.storage)

    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // Downloaded by the server at load time via plugin.yml's "libraries" instead of being shaded.
    compileOnly(libs.hikaricp)
    compileOnly(libs.sqlite.jdbc)
    compileOnly(libs.h2)
    compileOnly(libs.mysql.connector)
    compileOnly(libs.postgresql)
    compileOnly(libs.mongodb.driver.sync)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper.api)
    testImplementation(libs.hikaricp)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "hikariVersion" to libs.versions.hikari.get(),
            "sqliteVersion" to libs.versions.sqlite.get(),
            "h2Version" to libs.versions.h2.get(),
            "mysqlVersion" to libs.versions.mysql.get(),
            "postgresqlVersion" to libs.versions.postgresql.get(),
            "mongodbVersion" to libs.versions.mongodb.get(),
        )
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveClassifier = "plain"
    }

    shadowJar {
        archiveClassifier = ""
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
