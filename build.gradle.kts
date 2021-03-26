import kx.KxProject.*
import kx.LwjglModules.*
import kx.kxImplementation
import kx.lwjglImplementation

plugins {
    val build = "0.7.0+82"
    id("kx.kotlin.11") version build apply false
    id("kx.lwjgl") version build apply false
    id("kx.dokka") version build apply false
    id("kx.dokka.multimodule") version build
//    id("kx.publish") version build apply false
    `maven-publish`
    id("kx.snapshot") version "0.0.5"
    java
}

version = "0.7.9+25" // for ::bump

subprojects {
    apply(plugin = "kx.kotlin.11")
    apply(plugin = "kx.lwjgl")
    apply(plugin = "kx.dokka")
    apply(plugin = "kx.publish")
    apply(plugin = "java")

    version = rootProject.version
    group = "kotlin.graphics.uno"
}

project(":core") {
    dependencies {
        implementation(kotlin("reflect"))
        kxImplementation(unsigned, kool, glm, gli, gln)
        lwjglImplementation(glfw, jemalloc, opengl)
    }
}
project(":awt") {
    dependencies {
        implementation(rootProject.projects.core)
        kxImplementation(kool, glm, gln)
        lwjglImplementation(jawt, glfw, jemalloc, opengl)
    }
}
project(":vk") {
    dependencies {
        implementation(rootProject.projects.core)
        kxImplementation(kool, vkk)
        lwjglImplementation(glfw, jemalloc, opengl, vulkan)
    }
}

// limited dsl support inside here
extensions.configure<PublishingExtension>("publishing")  {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        suppressPomMetadataWarningsFor("runtimeElements")
    }
    repositories {
        maven {
            url = uri("mary")
        }
    }
}