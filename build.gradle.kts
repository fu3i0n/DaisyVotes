import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktlint by configurations.creating

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "wtf.amari"
version = "1.1"

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
        "kotlinStdlib" to "2.1.20",
        "placeholderApi" to "2.11.6",
        "kotlinCoroutines" to "1.10.2",
        "ktlint" to "1.5.0",
        "hikariCP" to "6.3.0",
        "sqlite" to "3.49.1.0",
        "exposed" to "0.61.0",
        "votifer" to "2.7.2",
    )

dependencies {
    compileOnly("io.papermc.paper:paper-api:${versions["paperApi"]}")
    compileOnly("me.clip:placeholderapi:${versions["placeholderApi"]}")
    compileOnly("com.github.NuVotifier:NuVotifier:${versions["votifer"]}")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions["kotlinStdlib"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions["kotlinCoroutines"]}")
    implementation("com.zaxxer:HikariCP:${versions["hikariCP"]}")
    implementation("org.xerial:sqlite-jdbc:${versions["sqlite"]}")
    implementation("org.jetbrains.exposed:exposed-core:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-dao:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${versions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-java-time:${versions["exposed"]}")

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
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            jvmTarget = targetJavaVersion.toString()
        }
    }
}

tasks {
    check {
        dependsOn(ktlintCheck)
    }
    build {
        dependsOn("shadowJar")
    }
    register<Copy>("copyDependencies") {
        from(configurations.runtimeClasspath)
        into("$buildDir/libs")
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
        inputs.properties(mapOf("version" to version))
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(mapOf("version" to version))
        }
    }
    withType<ShadowJar> {
        relocate("org.jetbrains.kotlinx", "wtf.amari.daisyVotes.libs.kotlinx")
        relocate("org.jetbrains.kotlin", "wtf.amari.daisyVotes.libs.kotlin")
    }
}
