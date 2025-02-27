import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.5.1"
val junitJupiterVersion = "5.7.1"
val mainClass = "no.nav.helse.sporbar.AppKt"

plugins {
    kotlin("jvm") version "1.5.30"
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.a77261b")

    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    implementation("com.zaxxer:HikariCP:4.0.2")
    implementation("no.nav:vault-jdbc:1.3.7")
    implementation("org.flywaydb:flyway-core:7.5.4")
    implementation("com.github.seratch:kotliquery:1.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("io.mockk:mockk:1.12.0")
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

tasks {

    named<KotlinCompile>("compileKotlin") {
        kotlinOptions.jvmTarget = "16"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "16"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }

    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
