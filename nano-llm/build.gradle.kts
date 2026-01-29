plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


dependencies {
    // NanoAndroid 模块依赖
    implementation(project(":nano-kernel"))
    implementation(project(":nano-framework"))
    implementation(project(":nano-app"))

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Network (for LLM API calls)
    implementation(libs.bundles.network)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
