plugins {
    java
}

group = "io.github.la8garlic"
val pluginVersion = providers.gradleProperty("pluginVersion").get()
val pluginProperties = mapOf("pluginVersion" to pluginVersion)
version = pluginVersion

dependencies {
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testCompileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    testRuntimeOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val integrationTest = sourceSets.create("integrationTest")

configurations[integrationTest.compileOnlyConfigurationName].extendsFrom(configurations.compileOnly.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    add(integrationTest.compileOnlyConfigurationName, sourceSets.main.get().output)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 25
    }

    processResources {
        inputs.properties(pluginProperties)
        expand(pluginProperties)
    }

    test {
        useJUnitPlatform()
    }

    val integrationProbeJar = register<Jar>("integrationProbeJar") {
        archiveBaseName.set("signlens-integration-probe")
        from(integrationTest.output)
    }

    register("integrationProbe") {
        dependsOn(integrationProbeJar)
        group = "verification"
        description = "Builds the test-only plugin used by the real Paper integration harness."
    }
}
