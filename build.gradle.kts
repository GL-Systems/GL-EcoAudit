plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "8.3.5"
    id("io.freefair.lombok") version "8.14"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    implementation("org.glstudio:Nexus:1.0-SNAPSHOT")

    // Vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // Database - HikariCP
    implementation("com.zaxxer:HikariCP:6.0.0")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // H2
    implementation("com.h2database:h2:2.2.224")

    // MySQL
    implementation("com.mysql:mysql-connector-j:8.3.0")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.1")

    // MongoDB
    implementation("org.mongodb:mongodb-driver-sync:5.1.0")
}

configurations {
    compileOnly {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
