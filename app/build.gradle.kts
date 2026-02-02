plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nano.android"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.nano.android"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // LLM 配置（从 gradle.properties 或环境变量读取）
        buildConfigField("String", "LLM_PROVIDER", "\"${project.findProperty("llm.provider") ?: "mock"}\"")
        buildConfigField("String", "LLM_API_KEY", "\"${project.findProperty("llm.apiKey") ?: ""}\"")
        buildConfigField("String", "LLM_MODEL", "\"${project.findProperty("llm.model") ?: ""}\"")
        buildConfigField("String", "LLM_BASE_URL", "\"${project.findProperty("llm.baseUrl") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = rootProject.extra["jvmTarget"] as String
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // 启用 BuildConfig
    }
}

dependencies {
    // NanoAndroid 模块
    implementation(project(":nano-kernel"))
    implementation(project(":nano-framework"))
    implementation(project(":nano-app"))
    implementation(project(":nano-view"))
    implementation(project(":nano-llm"))
    implementation(project(":nano-a2ui"))
    implementation(project(":nano-sample"))

    // AndroidX
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
