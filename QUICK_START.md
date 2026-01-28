# NanoAndroid 快速开始指南

## 快速安装

### 方式 1：使用 Gradle 构建并安装
```bash
# 构建并安装到连接的设备
./gradlew :app:installDebug

# 启动 app
adb shell am start -n com.nano.android/.shell.NanoShellActivity
```

### 方式 2：直接安装已构建的 APK
```bash
# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动 app
adb shell am start -n com.nano.android/.shell.NanoShellActivity
```

## 测试场景

### 1. 基础功能测试（Mock Provider）

**测试 1：计算器**
```
输入："计算 2 + 3"
预期：显示文本 "5" + A2UI Card（标题 "计算结果"）
```

**测试 2：笔记**
```
输入："新增笔记"
预期：显示 A2UI Card（笔记详情）
```

**测试 3：列出笔记**
```
输入："显示我的笔记"
预期：显示 A2UI List（所有笔记）
```

### 2. 真实 LLM 测试（需配置 API Key）

**前提**：编辑 `gradle.properties`，设置 `llm.provider=groq` 和 `llm.apiKey`

**测试 1：复杂表达式**
```
输入："帮我算一下 10 乘以 5"
预期：理解"乘以"，返回 "50"
```

**测试 2：自然语言笔记**
```
输入："记录一下今天的会议内容：讨论了项目进度"
预期：创建标题为 "今天的会议内容" 的笔记
```

**测试 3：澄清问题**
```
输入："帮我处理一下"
预期：返回澄清问题 "您想让我帮您处理什么？"
```

## 查看日志

### 实时查看 NanoAndroid 日志
```bash
adb logcat | grep "\[Nano\]"
```

### 关键日志检查点

**系统启动**
```
[Nano] NanoApplication onCreate
[Nano] Creating NanoLLMService...
[Nano] LLM Config - Provider: groq, Model: mixtral-8x7b-32768, ...
[Nano] NanoLLMService created and started
[Nano] Registered CalculatorAgent: calculator
[Nano] Registered NotepadAgent: notepad
```

**处理用户输入**
```
[Nano] Processing user input: 计算 2 + 3
[Nano] Intent parsed: IntentUnderstanding(targetApps=[calculator], ...)
[Nano] Executing agent: calculator
[Nano] Rendering A2UI: A2UICard
[Nano] A2UI rendered successfully: A2UICard
[Nano] Response displayed
```

## 配置 Groq（推荐）

### 步骤 1：获取 API Key
1. 访问 https://console.groq.com
2. 注册账号
3. 创建 API Key（免费）

### 步骤 2：配置项目
编辑 `gradle.properties`：
```properties
llm.provider=groq
llm.apiKey=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=mixtral-8x7b-32768
```

### 步骤 3：重新构建
```bash
./gradlew clean :app:installDebug
```

### 步骤 4：验证
查看启动日志：
```bash
adb logcat | grep "LLM Config"
```

预期输出：
```
[Nano] LLM Config - Provider: groq, Model: mixtral-8x7b-32768, BaseUrl: https://api.groq.com/openai/v1
```

## 常用命令

### 构建相关
```bash
# 清理构建
./gradlew clean

# 构建 debug APK
./gradlew :app:assembleDebug

# 构建并安装
./gradlew :app:installDebug

# 运行测试
./gradlew test

# 运行特定模块的测试
./gradlew :nano-llm:test
```

### 设备相关
```bash
# 查看连接的设备
adb devices

# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 卸载应用
adb uninstall com.nano.android

# 启动 app
adb shell am start -n com.nano.android/.shell.NanoShellActivity

# 停止 app
adb shell am force-stop com.nano.android

# 清除应用数据
adb shell pm clear com.nano.android
```

### 调试相关
```bash
# 查看所有日志
adb logcat

# 只看 NanoAndroid 日志
adb logcat | grep "\[Nano\]"

# 清除日志缓冲区
adb logcat -c

# 保存日志到文件
adb logcat | grep "\[Nano\]" > nano.log
```

## 故障排查

### 问题 1：BuildConfig 找不到
**症状**：编译错误 `Unresolved reference: BuildConfig`

**解决**：
1. 确认 `app/build.gradle.kts` 中有 `buildConfig = true`
2. 运行 `./gradlew clean build`

### 问题 2：API Key 无效
**症状**：LLM 调用失败，返回 401 错误

**解决**：
1. 验证 `gradle.properties` 中的 `llm.apiKey` 是否正确
2. 检查 API Key 是否有效（访问 Provider 的控制台）
3. 重新构建：`./gradlew clean :app:installDebug`

### 问题 3：A2UI 没有显示
**症状**：只有文本响应，没有 UI 组件

**解决**：
1. 查看日志：`adb logcat | grep "A2UI"`
2. 检查是否有错误：`[Nano] Failed to render A2UI`
3. 确认 Agent 返回了 `a2ui` 字段（非 null）

### 问题 4：网络连接失败
**症状**：LLM 调用超时

**解决**：
1. 检查设备网络连接
2. 确认 baseUrl 可访问
3. 尝试更换 Provider（如从 openai 换到 groq）

## 文件结构

```
nanoAndroid/
├── app/
│   ├── build.gradle.kts          # BuildConfig 配置
│   └── src/main/
│       ├── java/com/nano/android/shell/
│       │   ├── NanoApplication.kt      # LLM 配置
│       │   ├── NanoShellActivity.kt    # UI 渲染集成
│       │   └── NanoViewConverter.kt    # NanoView → Android View
│       └── res/layout/
│           └── activity_shell.xml      # 布局文件（含 a2uiContainer）
├── nano-llm/
│   └── src/main/java/com/nano/llm/
│       └── intent/
│           └── IntentParser.kt         # 优化后的 Prompt
├── gradle.properties                   # LLM 配置（已 .gitignore）
├── gradle.properties.example           # 配置示例
├── UI_RENDERING_LLM_INTEGRATION.md     # 详细实施报告
└── QUICK_START.md                      # 本文件
```

## 技术支持

### 文档
- 详细实施报告：[UI_RENDERING_LLM_INTEGRATION.md](UI_RENDERING_LLM_INTEGRATION.md)
- 项目指南：[CLAUDE.md](CLAUDE.md)
- 配置示例：[gradle.properties.example](gradle.properties.example)

### 获取 API Key
- Groq: https://console.groq.com/keys
- OpenRouter: https://openrouter.ai/keys
- Together: https://api.together.xyz/settings/api-keys
- OpenAI: https://platform.openai.com/api-keys

### 推荐配置
**开发环境**：Mock Provider（默认，无需配置）
**测试环境**：Groq（免费，快速，推荐）
**生产环境**：根据需求选择（建议后端代理）

## 下一步

1. ✅ 完成基础功能验证（Mock Provider）
2. ✅ 配置真实 LLM（Groq）
3. ✅ 测试复杂自然语言输入
4. 🔜 添加更多 Agent（天气、新闻等）
5. 🔜 实现多轮对话
6. 🔜 支持本地模型（Ollama）

---

**祝你使用愉快！** 🎉
