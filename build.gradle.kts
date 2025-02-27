import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.10"
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "1.7.2" // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
}

group = "me.nukmuk"
version = "2.0.0"

repositories {
    mavenCentral()
//    maven("https://repo.papermc.io/repository/maven-public/") {
//        name = "papermc-repo"
//    }
//    maven("https://oss.sonatype.org/content/groups/public/") {
//        name = "sonatype"
//    }
    maven(url = "https://repo.codemc.org/repository/maven-public/")
}

dependencies {
//    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.5.2")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:9.5.3")
    implementation(kotlin("reflect"))
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.withType<ShadowJar> {
    relocate("dev.jorel.commandapi", "$group.commandapi")
//    minimize()
    archiveFileName.set("Sheepy.jar")
    destinationDirectory.set(file("F:/Servers/dev/plugins"))
}