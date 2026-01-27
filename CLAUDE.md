# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NanoAndroid is a simplified Android framework that runs as a real Android app, designed for:
1. Educational understanding of Android system architecture
2. Experimenting with LLM integration into operating systems (Natural Language API + A2UI protocol)

**Language**: Kotlin | **Min SDK**: 26 | **Target SDK**: 34 | **Build**: Gradle 8.x with Kotlin DSL

## Build Commands

```bash
# Build the project
./gradlew build

# Build specific module
./gradlew :nano-kernel:build

# Install debug APK
./gradlew :app:installDebug

# Run unit tests
./gradlew test

# Run tests for specific module
./gradlew :nano-kernel:test

# Clean build
./gradlew clean build
```

## Architecture

### Module Dependency Graph

```
app (Shell)
 └── nano-kernel ← nano-framework ← nano-app ← nano-view
                         ↑              ↑
                    nano-llm ←── nano-a2ui
                                      ↑
                               nano-sample
```

### Module Responsibilities

| Module | Purpose |
|--------|---------|
| `nano-kernel` | Core IPC (NanoBinder, NanoParcel, NanoServiceManager) and messaging (NanoHandler, NanoLooper) |
| `nano-framework` | System services: NanoSystemServer, AMS, WMS, PMS |
| `nano-app` | App framework: NanoActivity, NanoService, NanoContext, NanoIntent |
| `nano-view` | View system: NanoView, NanoViewGroup, widgets |
| `nano-llm` | LLM integration: NanoLLMService, NaturalLanguageAPI, LLMProviders (OpenAI/Claude/Local) |
| `nano-a2ui` | A2UI protocol for cross-app AI interaction |
| `nano-sample` | Sample apps (Calculator, Notepad) |

### Key Design Patterns

**Binder IPC Simulation**: Uses in-memory calls (not kernel driver) but maintains the same transaction-based API:
```
App → INanoActivityManager.startActivity()
    → Proxy.transact(code, data, reply, flags)
    → NanoBinder.transact() → NanoAMS.onTransact()
```

**System Service Startup Order**:
```
NanoApplication.onCreate()
  → NanoSystemServer.run()
  → startBootstrapServices()  // PMS, AMS
  → startCoreServices()       // WMS
  → startOtherServices()      // LLMService
  → systemReady()             // Start Launcher
```

**Service Dependency Chain**: PMS → AMS → WMS → LLMService

### Key Files

- `nano-kernel/.../binder/NanoBinder.kt` - Base class for all Binder services
- `nano-kernel/.../binder/NanoServiceManager.kt` - Service registry (singleton)
- `nano-kernel/.../handler/NanoHandler.kt` - Message handler (like Android Handler)
- `nano-framework/.../server/NanoSystemServer.kt` - Boots all system services
- `app/.../shell/NanoShellActivity.kt` - Main container that bridges to real Android Views

### Simplifications vs Real Android

- **IPC**: In-memory calls instead of kernel Binder driver
- **Process model**: Single process with threads (no real process isolation)
- **Rendering**: Delegates to real Android View system via FrameLayout containers
- **Zygote**: Simple class instantiation instead of process forking
