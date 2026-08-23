plugins {
    java
}

group = "io.github.la8garlic"
val pluginVersion = providers.gradleProperty("pluginVersion").get()
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

    test {
        useJUnitPlatform()
    }
}
