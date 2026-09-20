plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kord.core)
    implementation(libs.dotenv.kotlin)
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.guava)
    runtimeOnly(libs.slf4j.simple)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.bundles.koin.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "org.solync.idealync.MainKt"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
