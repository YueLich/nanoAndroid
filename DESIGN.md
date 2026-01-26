# NanoAndroid 技术设计文档

## 核心组件详细设计

### 1. NanoBinder - IPC 通信机制

#### 设计原理
真实 Android 的 Binder 是基于内核驱动的跨进程通信机制。NanoBinder 通过内存直接调用来模拟，但保持相同的接口设计和事务模型。

#### 核心类

**NanoBinder.kt**
```kotlin
abstract class NanoBinder : INanoInterface {
    // 事务处理
    abstract fun onTransact(code: Int, data: NanoParcel, reply: NanoParcel?, flags: Int): Boolean

    // 同步/异步调用
    fun transact(code: Int, data: NanoParcel, reply: NanoParcel?, flags: Int): Boolean
}
```

**NanoServiceManager.kt**
```kotlin
object NanoServiceManager {
    private val services = ConcurrentHashMap<String, NanoBinder>()

    fun addService(name: String, service: NanoBinder)
    fun getService(name: String): NanoBinder?
    fun waitForService(name: String, callback: (NanoBinder) -> Unit)
}
```

**NanoParcel.kt**
```kotlin
class NanoParcel {
    // 写入操作
    fun writeInt(value: Int)
    fun writeString(value: String?)
    fun writeParcelable(p: NanoParcelable?)

    // 读取操作
    fun readInt(): Int
    fun readString(): String?
    fun readParcelable<T>(): T?
}
```

#### 调用流程
```
App -> INanoActivityManager.startActivity()
     -> Proxy.transact(TRANSACTION_START_ACTIVITY, data, reply, 0)
     -> NanoBinder.transact()
     -> NanoAMS.onTransact()
     -> NanoAMS.startActivity()
```

---

### 2. NanoSystemServer - 系统服务启动

#### 启动流程
```
NanoApplication.onCreate()
  -> NanoSystemServer.run()
  -> startBootstrapServices()
       - PackageManagerService (PMS)
       - ActivityManagerService (AMS)
  -> startCoreServices()
       - WindowManagerService (WMS)
  -> startOtherServices()
       - LLMService
  -> onSystemReady()
       - 通知各服务系统就绪
       - 启动 Launcher
```

#### 服务依赖关系
```
PMS (无依赖)
  ↓
AMS (依赖 PMS)
  ↓
WMS (依赖 AMS)
  ↓
LLMService (依赖 AMS)
```

---

### 3. NanoActivityManagerService - Activity 管理

#### 核心职责
1. Activity 生命周期管理
2. 任务栈（Task Stack）管理
3. Intent 解析和路由
4. 进程管理（简化版）

#### 数据结构

**NanoActivityRecord**
```kotlin
data class NanoActivityRecord(
    val token: String,                    // 唯一标识
    val intent: NanoIntent,               // 启动意图
    val activityInfo: NanoActivityInfo,   // 组件信息
    var activity: NanoActivity?,          // Activity 实例
    var state: LifecycleState,            // 生命周期状态
    var task: NanoTaskRecord?,            // 所属任务
    var window: NanoWindowState?          // 关联窗口
)
```

**NanoTaskRecord**
```kotlin
data class NanoTaskRecord(
    val taskId: Int,
    val affinity: String,
    val activities: MutableList<NanoActivityRecord>
)
```

#### Activity 启动流程
```
1. AMS.startActivity(intent)
2. 解析 Intent，找到目标 Activity
3. 检查权限
4. 创建 ActivityRecord
5. 确定任务栈（新建或复用）
6. 暂停当前 Activity
7. 请求 WMS 创建窗口
8. 实例化 Activity
9. 执行生命周期：onCreate -> onStart -> onResume
```

#### 生命周期状态机
```
INITIALIZED
    ↓ onCreate()
CREATED
    ↓ onStart()
STARTED
    ↓ onResume()
RESUMED
    ↓ onPause()
STARTED
    ↓ onStop()
CREATED
    ↓ onDestroy()
DESTROYED
```

---

### 4. NanoWindowManagerService - 窗口管理

#### 核心职责
1. 窗口的创建和销毁
2. Z-order（层级）管理
3. 窗口布局计算
4. 触摸事件分发

#### 数据结构

**NanoWindowState**
```kotlin
data class NanoWindowState(
    val token: String,
    val type: Int,                      // APPLICATION, SYSTEM_ALERT, etc.
    val activityRecord: NanoActivityRecord?,
    var layer: Int,                     // Z-order 层级
    var isFocused: Boolean,
    var isVisible: Boolean,
    val frame: Rect,                    // 窗口位置和大小
    var viewContainer: ViewGroup?       // 真实 Android View 容器
)
```

#### 窗口类型和层级
```kotlin
const val TYPE_BASE_APPLICATION = 1      // 层级 1
const val TYPE_APPLICATION = 2           // 层级 2+
const val TYPE_TOAST = 2005              // 层级 1050
const val TYPE_SYSTEM_ALERT = 2003       // 层级 1100
const val TYPE_INPUT_METHOD = 2011       // 层级 1200
```

#### 事件分发流程
```
MotionEvent
  -> WMS.dispatchTouchEvent()
  -> 从顶层窗口开始遍历（Z-order 降序）
  -> window.containsPoint(x, y)
  -> window.viewContainer.dispatchTouchEvent()
```

---

### 5. NanoLLMService - LLM 系统服务

#### 架构设计
```
NaturalLanguageAPI (应用层)
    ↓
INanoLLM (Binder 接口)
    ↓
NanoLLMService (系统服务)
    ↓
LLMProvider (抽象层)
    ↓
OpenAIProvider / ClaudeProvider / LocalLLMProvider
```

#### 核心流程

**1. 自然语言命令执行**
```
User: "打开计算器"
  ↓
1. NaturalLanguageAPI.execute(command)
2. 收集系统上下文（运行的应用、系统状态）
3. 构建系统提示词 + 可用函数列表
4. 调用 LLM（Function Calling）
5. 解析 LLM 响应，提取意图
6. 执行对应的系统操作
7. 返回结果
```

**2. 系统提示词模板**
```
你是 NanoAndroid 系统的 AI 助手。

当前系统状态：
- 运行的应用：[Calculator, Notepad]
- 前台应用：Calculator
- 可用应用：[Calculator, Notepad, Settings, Browser]
- 电池电量：85%
- 网络状态：WiFi

可调用函数：
- launch_app(package_name, action?)
- set_volume(level, stream)
- send_notification(title, content)
- query_apps(filter?)
- a2ui_action(target_app, action, parameters)

用户输入：{user_command}
```

**3. 意图类型**
```kotlin
sealed class ParsedIntent {
    data class SystemAction(val action: SystemActionType, val parameters: Map<String, String>)
    data class AppLaunch(val packageName: String, val activityName: String, val extras: Bundle?)
    data class A2UIAction(val targetApp: String, val action: String, val parameters: Map<String, String>)
    data class Conversation(val response: String)
    object Unknown
}
```

#### LLM Provider 接口
```kotlin
interface LLMProvider {
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ConversationMessage>,
        functions: List<LLMFunction>
    ): LLMResponse
}

data class LLMFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, String>
)

data class LLMResponse(
    val type: ResponseType,  // TEXT, FUNCTION_CALL
    val content: String?,
    val functionCall: FunctionCall?
)
```

---

### 6. A2UI 协议 - 跨应用智能交互

#### 协议层次
```
应用层：A2UIActivity (应用实现 A2UI 能力)
    ↓
消息层：A2UIMessage (标准化消息格式)
    ↓
路由层：A2UIBridge (消息路由和权限控制)
    ↓
执行层：UIAccessibilityBridge (降级方案)
```

#### 消息类型

**1. 能力查询**
```kotlin
Request:
{
  type: CAPABILITY_QUERY,
  targetApp: "com.nano.notepad"
}

Response:
{
  type: CAPABILITY_RESPONSE,
  payload: {
    supportedActions: ["create", "edit", "delete", "search"],
    exposedElements: [
      {
        id: "new_note_button",
        semanticName: "新建笔记按钮",
        actions: ["click"]
      }
    ]
  }
}
```

**2. UI 查询**
```kotlin
Request:
{
  type: UI_QUERY,
  targetApp: "com.nano.notepad",
  payload: {
    queryType: "hierarchy"
  }
}

Response:
{
  type: UI_EVENT,
  payload: {
    hierarchy: {
      elements: [
        {
          id: "note_list",
          className: "RecyclerView",
          semanticRole: "list",
          children: [...]
        }
      ]
    }
  }
}
```

**3. UI 操作**
```kotlin
Request:
{
  type: UI_ACTION,
  targetApp: "com.nano.notepad",
  payload: {
    action: "create",
    selector: {
      type: "semantic",
      value: "新建笔记按钮"
    },
    parameters: {
      "title": "购物清单",
      "content": "牛奶、面包、鸡蛋"
    }
  }
}

Response:
{
  type: UI_EVENT,
  payload: {
    success: true,
    message: "笔记已创建",
    changedElements: ["note_list"]
  }
}
```

#### 元素选择器类型

**1. ID 选择器**
```kotlin
ElementSelector(type = "id", value = "new_note_button")
```

**2. 文本选择器**
```kotlin
ElementSelector(type = "text", value = "新建笔记")
```

**3. 语义选择器（AI 增强）**
```kotlin
ElementSelector(type = "semantic", value = "创建新笔记的按钮")
// LLM 理解语义，找到最匹配的元素
```

**4. 组合选择器**
```kotlin
ElementSelector(
    type = "text",
    value = "编辑",
    parent = ElementSelector(type = "id", value = "note_card_123")
)
```

#### A2UI 安全机制

**1. 权限声明**
```kotlin
// 应用需要在 AndroidManifest 中声明
<meta-data
    android:name="nano.a2ui.capabilities"
    android:resource="@xml/a2ui_capabilities" />
```

**2. 权限请求**
```kotlin
// 用户授权才能跨应用操作
A2UIPermission.request(
    sourceApp = "system.llm",
    targetApp = "com.nano.notepad",
    actions = listOf("create", "edit")
)
```

**3. 敏感操作确认**
```kotlin
// 删除等危险操作需要用户确认
if (action == "delete") {
    showConfirmationDialog("是否允许删除笔记？")
}
```

---

## 技术挑战与解决方案

### 挑战 1：在单进程内模拟多进程
**解决方案**：
- 使用 `ThreadLocal` 隔离不同"进程"的上下文
- 用线程池模拟不同的进程
- 保持 Binder 接口设计不变

### 挑战 2：LLM 响应延迟
**解决方案**：
- 显示加载动画，提升用户体验
- 缓存常用命令的解析结果
- 支持流式输出（Streaming）
- 提供本地小模型作为降级方案

### 挑战 3：A2UI 元素识别准确性
**解决方案**：
- 应用主动暴露关键元素（ExposedElement）
- 使用 ContentDescription 增强语义信息
- LLM 辅助理解 UI 结构
- 降级到 Accessibility Service

### 挑战 4：窗口管理与真实 View 系统的融合
**解决方案**：
- WMS 管理窗口层级和生命周期
- 每个窗口对应一个 FrameLayout 容器
- 窗口的 Z-order 通过 View.elevation 实现
- 触摸事件由 WMS 拦截并分发

---

## 性能优化策略

### 1. Binder 调用优化
- 对象池复用 NanoParcel
- 避免不必要的序列化
- 异步调用使用 FLAG_ONEWAY

### 2. 窗口渲染优化
- 只渲染可见窗口
- 使用硬件加速
- 减少布局层次

### 3. LLM 调用优化
- 缓存系统上下文
- 合并批量请求
- 使用更小的模型处理简单命令

### 4. 内存管理
- 及时释放不可见窗口的资源
- 限制任务栈深度
- 清理对话历史

---

## 测试策略

### 1. 单元测试
- Binder 事务处理测试
- Intent 解析测试
- 生命周期状态机测试
- A2UI 消息路由测试

### 2. 集成测试
- 完整的 Activity 启动流程
- 跨应用 A2UI 交互
- LLM 命令端到端测试

### 3. 性能测试
- Activity 启动速度
- 窗口切换流畅度
- LLM 响应时间

---

## 未来扩展方向

### 1. 更多系统服务
- NotificationManagerService
- InputMethodService
- LocationManagerService

### 2. 更强大的 A2UI
- 支持手势识别
- 支持拖拽操作
- 支持多模态交互（语音 + 触摸）

### 3. Agent 协作
- 多 Agent 协同完成复杂任务
- Agent 之间的消息传递
- Agent 的学习和进化

### 4. 本地化增强
- 完全离线的 LLM 推理
- 边缘计算优化
- 隐私保护
