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

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}
