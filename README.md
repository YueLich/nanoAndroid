# NanoAndroid

极简化的 Android 框架项目，用于理解 Android 核心机制，并探索 LLM 与操作系统的深度融合。

## 项目目标

1. **学习目的**：通过极度简化的实现，帮助理解 Android 系统架构核心逻辑
2. **LLM 实验**：探索大模型如何融入操作系统，实现自然语言系统 API 和跨应用智能交互

## 核心特性

### 系统服务架构
- **NanoBinder**：简化版 Binder IPC 机制（内存模拟）
- **NanoSystemServer**：系统服务启动器
- **NanoActivityManagerService (AMS)**：Activity 生命周期和任务栈管理
- **NanoWindowManagerService (WMS)**：窗口管理和事件分发
- **NanoPackageManagerService (PMS)**：应用包管理

### LLM 集成
- **自然语言系统 API**：用自然语言调用系统功能
  ```kotlin
  NaturalLanguageAPI.execute("打开计算器")
  NaturalLanguageAPI.execute("把音量调到50%")
  NaturalLanguageAPI.execute("在记事本里新建一个笔记，标题是购物清单")
  ```
- **多 LLM 支持**：支持 OpenAI、Claude、本地模型

### A2UI 协议 (Agent-to-UI)
跨应用智能交互协议，允许 AI Agent 理解和操作任意应用的 UI
- 语义化的 UI 元素描述
- 标准化的动作接口
- 跨应用消息路由
- 权限控制机制

## 技术栈

- **语言**：Kotlin
- **运行环境**：真实 Android App（作为沙箱运行）
- **最低 Android 版本**：API 26 (Android 8.0)
- **构建工具**：Gradle 8.x + Kotlin DSL

## 项目结构

```
nanoAndroid/
├── app/                          # 主应用 Shell
├── nano-kernel/                  # 内核层
│   ├── binder/                   # Binder IPC 机制
│   ├── handler/                  # Handler/Looper 消息循环
│   └── process/                  # 进程抽象
├── nano-framework/               # 框架层
│   ├── server/                   # SystemServer
│   ├── am/                       # ActivityManagerService
│   ├── wm/                       # WindowManagerService
│   └── pm/                       # PackageManagerService
├── nano-app/                     # 应用框架层
│   ├── NanoActivity.kt
│   ├── NanoService.kt
│   ├── NanoContext.kt
│   └── NanoIntent.kt
├── nano-view/                    # 视图系统
│   ├── NanoView.kt
│   ├── NanoViewGroup.kt
│   └── widget/                   # 基础控件
├── nano-llm/                     # LLM 集成层
│   ├── core/                     # LLM 系统服务
│   ├── api/                      # 自然语言 API
│   └── provider/                 # LLM 提供者（OpenAI/Claude/Local）
├── nano-a2ui/                    # A2UI 协议层
│   ├── protocol/                 # 协议定义
│   ├── bridge/                   # 跨应用桥接
│   └── agent/                    # AI Agent 基础框架
└── nano-sample/                  # 示例应用
    ├── calculator/
    └── notepad/
```

## 实现路线图

### Phase 1: 基础设施 ✓
- [x] 项目结构初始化
- [ ] NanoBinder / NanoParcel 实现
- [ ] NanoServiceManager 服务注册/查找
- [ ] NanoHandler / NanoLooper 消息机制
- [ ] NanoContext / NanoIntent 基础实现

### Phase 2: 系统服务
- [ ] NanoSystemServer 启动流程
- [ ] NanoPackageManagerService
- [ ] NanoActivityManagerService（Activity 生命周期）
- [ ] NanoWindowManagerService（窗口管理）
- [ ] 任务栈管理

### Phase 3: 应用框架
- [ ] NanoActivity 完整实现
- [ ] NanoService 实现
- [ ] NanoView / NanoViewGroup
- [ ] 基础 Widget（TextView、Button、EditText、LinearLayout）
- [ ] 布局系统
- [ ] 示例应用：Calculator

### Phase 4: LLM 集成
- [ ] LLMProvider 接口定义
- [ ] OpenAI / Claude Provider 实现
- [ ] NanoLLMService 系统服务
- [ ] 系统上下文收集器
- [ ] 意图解析器
- [ ] 命令执行器
- [ ] NaturalLanguageAPI 封装

### Phase 5: A2UI 协议
- [ ] A2UI 协议定义
- [ ] A2UIMessage 消息格式
- [ ] A2UIBridge 跨应用桥接
- [ ] A2UIRouter 消息路由
- [ ] A2UIActivity 基类
- [ ] 跨应用交互示例

### Phase 6: 优化与文档
- [ ] 性能优化
- [ ] 单元测试
- [ ] API 文档
- [ ] 架构文档
- [ ] 开发者指南

## 核心设计理念

### 简化但不失真
- 保持 Android 核心概念和 API 设计
- 去除不必要的复杂性
- 用简单方式实现核心机制

### 与真实 Android 的区别

| 真实 Android | NanoAndroid | 简化原因 |
|------------|-------------|---------|
| Binder 内核驱动 | 内存直接调用 | 无法在 App 中实现内核驱动 |
| 多进程隔离 | 单进程模拟 | 简化，教学目的 |
| Zygote 进程孵化 | 类实例化 | 无需真实进程创建 |
| SurfaceFlinger | 真实 Android View | 复用现有渲染 |
| 完整权限系统 | 简化权限检查 | 沙箱内运行 |

### LLM 融合原则
- **系统级集成**：LLM 作为系统服务，而非应用层功能
- **自然交互**：用自然语言替代传统 API 调用
- **智能路由**：LLM 理解意图并路由到正确的组件
- **安全可控**：危险操作需要确认，支持撤销

## 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高
- JDK 17
- Android SDK API 34

### 构建项目
```bash
git clone <repository-url>
cd nanoAndroid
./gradlew build
```

### 运行示例
```bash
./gradlew :app:installDebug
adb shell am start com.nano.android/.NanoShellActivity
```

## 示例代码

### 创建一个 NanoActivity

```kotlin
class CalculatorActivity : NanoActivity() {

    override fun onCreate(savedInstanceState: NanoBundle?) {
        super.onCreate(savedInstanceState)

        val layout = NanoLinearLayout(this).apply {
            orientation = NanoLinearLayout.VERTICAL
        }

        val display = NanoTextView(this).apply {
            text = "0"
            textSize = 48f
        }

        val button = NanoButton(this).apply {
            text = "Calculate"
            setOnClickListener {
                // 处理点击
            }
        }

        layout.addView(display)
        layout.addView(button)

        setContentView(layout)
    }
}
```

### 使用自然语言 API

```kotlin
// 在任何地方调用
lifecycleScope.launch {
    val result = NaturalLanguageAPI.getInstance(context)
        .execute("打开计算器并计算 123 + 456")

    when (result) {
        is NLCommandResult.Success -> {
            // 命令执行成功
        }
        is NLCommandResult.Error -> {
            // 处理错误
        }
    }
}
```

### 创建 A2UI-Ready Activity

```kotlin
class NotepadActivity : A2UIActivity() {

    override fun declareCapability() = A2UIPayload.Capability(
        supportedActions = listOf(
            A2UIProtocol.Actions.CREATE,
            A2UIProtocol.Actions.EDIT,
            A2UIProtocol.Actions.DELETE,
            A2UIProtocol.Actions.SEARCH
        ),
        exposedElements = listOf(
            ExposedElement(
                id = "new_note_button",
                semanticName = "新建笔记按钮",
                description = "创建新笔记",
                actions = listOf("click")
            )
        )
    )

    override fun executeUIAction(action: A2UIPayload.UIAction): A2UIPayload.ActionResult {
        return when (action.action) {
            A2UIProtocol.Actions.CREATE -> {
                // 创建笔记逻辑
                A2UIPayload.ActionResult(success = true, message = "笔记已创建")
            }
            else -> A2UIPayload.ActionResult(success = false, message = "不支持的操作")
        }
    }
}
```

## 贡献指南

欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- Issues: GitHub Issues
- Discussions: GitHub Discussions

---

**注意**：这是一个学习和实验性项目，不应用于生产环境。
