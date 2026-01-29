plugins {
    kotlin("jvm")
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
    implementation(project(":nano-view"))
    implementation(project(":nano-llm"))
    implementation(project(":nano-a2ui"))

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
