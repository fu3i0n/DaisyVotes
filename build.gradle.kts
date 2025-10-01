import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktlint by configurations.creating

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.2.2"
}

group = "cat.daisy"
version = "1.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

val versions =
    mapOf(
        "paperApi" to "1.21.4-R0.1-SNAPSHOT",
        "kotlin" to "2.1.20", // 🔹 renamed to match usage
        "placeholderApi" to "2.11.6",
        "kotlinCoroutines" to "1.10.2",
        "ktlint" to "1.7.1",
        "hikariCP" to "7.0.2",
        "sqlite" to "3.50.3.0",
        "exposed" to "0.61.0",
        "votifer" to "2.7.2",
    )

dependencies {
    compileOnly("io.papermc.paper:paper-api:${versions["paperApi"]}")
    compileOnly("me.clip:placeholderapi:${versions["placeholderApi"]}")
    compileOnly("com.github.NuVotifier:NuVotifier:${versions["votifer"]}")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions["kotlin"]}")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions["kotlinCoroutines"]}")

    implementation("org.jetbrains.exposed:exposed-core:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-dao:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-java-time:${versions["exposed"]}")

    // Database dependencies - these will be downloaded by the server via plugin.yml
    compileOnly("com.zaxxer:HikariCP:${versions["hikariCP"]}") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    compileOnly("org.xerial:sqlite-jdbc:${versions["sqlite"]}") {
        exclude(group = "org.xerial", module = "sqlite-jdbc-macros")
    }

    ktlint("com.pinterest.ktlint:ktlint-cli:${versions["ktlint"]}") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/src/**/*.kt", "**.kts", "!**/build/**")
}

val targetJavaVersion = 21

kotlin {
    jvmToolchain(targetJavaVersion)
    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
}

// ✅ New compilerOptions DSL
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
    }
}

tasks {
    check {
        dependsOn(ktlintCheck)
    }

    build {
        dependsOn("shadowJar")
        finalizedBy("printJarSize")
    }

    register<Copy>("copyDependencies") {
        from(configurations.runtimeClasspath)
        into(layout.buildDirectory.dir("libs"))
    }

    register<JavaExec>("ktlintFormat") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Check Kotlin code style and format"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        args("-F", "**/src/**/*.kt", "**.kts", "!**/build/**")
    }

    processResources {
        val versionValue = project.version.toString()
        inputs.property("version", versionValue)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(mapOf("version" to versionValue))
        }
    }

    withType<ShadowJar> {
        archiveClassifier.set("shaded")

        minimize {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        }

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/maven/**")
        exclude("META-INF/versions/**")
        exclude("META-INF/services/javax.*")
        exclude("**/*.html")
        exclude("**/*.txt")
        exclude("**/*.properties")
        exclude("**/*.kotlin_module")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_builtins")

        mergeServiceFiles()

        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
            )
        }
    }

    register("printJarSize") {
        dependsOn("shadowJar")
        doLast {
            val libsDir = layout.buildDirectory.dir("libs").get().asFile
            val jarFiles = libsDir.listFiles { file ->
                file.name.endsWith("-shaded.jar")
            }

            if (jarFiles != null && jarFiles.isNotEmpty()) {
                val jarFile = jarFiles.first()
                val sizeInMB = jarFile.length() / (1024.0 * 1024.0)
                println("Final JAR size: ${String.format("%.2f", sizeInMB)} MB")
                println("JAR location: ${jarFile.absolutePath}")
            } else {
                println("No shaded JAR files found in ${libsDir.absolutePath}")
            }
        }
    }
}