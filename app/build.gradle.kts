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

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
