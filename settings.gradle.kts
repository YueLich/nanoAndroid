pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NanoAndroid"

// 主应用模块
include(":app")

// 核心模块
include(":nano-kernel")      // Binder IPC, Handler/Looper
include(":nano-framework")   // SystemServer, AMS, WMS, PMS
include(":nano-app")         // Activity, Service, Context, Intent
include(":nano-view")        // View系统, Widget

// LLM 集成模块
include(":nano-llm")         // LLM服务, 自然语言API

// A2UI 协议模块
include(":nano-a2ui")        // 跨应用智能交互协议

// 示例模块
include(":nano-sample")      // 示例应用
