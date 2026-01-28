# NanoLLM 模块设计方案

## 1. 核心理念

NanoLLM 采用 **多 Agent 协作架构**：

- **System Agent（系统智能体）**：作为用户的统一入口，负责意图理解、任务分发、UI 渲染协调
- **App Agent（应用智能体）**：每个应用可以拥有自己的 Agent，处理领域特定的任务
- **A2UI 协议**：统一的 AI 到 UI 渲染协议，支持两种模式的 UI 生成

```
┌─────────────────────────────────────────────────────────────────────┐
│                            用户                                      │
│                    "帮我在美团点一份黄焖鸡"                            │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      System Agent (系统智能体)                        │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  1. 意图理解: 用户想点外卖，目标app=美团，商品=黄焖鸡          │    │
│  │  2. 路由决策: 需要与美团 App Agent 通信                       │    │
│  │  3. 构建请求: AgentMessage { intent, entities, context }    │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │ Agent 通信协议
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    美团 App Agent (应用智能体)                        │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  1. 理解任务: 搜索黄焖鸡相关商品                               │    │
│  │  2. 执行业务: 调用搜索API，获取商品列表                        │    │
│  │  3. 返回结果: 商品数据 + (可选)A2UI JSON                      │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         UI 渲染 (两种模式)                           │
│                                                                     │
│  模式 A: App Agent 生成 A2UI JSON                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ App Agent 返回:                                              │   │
│  │ {                                                            │   │
│  │   "type": "list",                                            │   │
│  │   "data": [...商品数据...],                                   │   │
│  │   "a2ui": { "template": "product_list", "items": [...] }     │   │
│  │ }                                                            │   │
│  │ → 系统直接按 A2UI 协议渲染                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  模式 B: System Agent 生成 A2UI JSON                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ App Agent 返回:                                              │   │
│  │ {                                                            │   │
│  │   "type": "raw_data",                                        │   │
│  │   "data": [...商品数据...]                                    │   │
│  │ }                                                            │   │
│  │ → System Agent 根据数据 + 上下文生成 A2UI JSON                 │   │
│  │ → 系统按 A2UI 协议渲染                                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         NanoAndroid 系统                            │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    NanoLLMService (系统服务)                    │ │
│  │                                                               │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │              System Agent (系统智能体)                    │ │ │
│  │  │                                                         │ │ │
│  │  │  • 自然语言理解 (NLU)                                    │ │ │
│  │  │  • 多 Agent 选择与筛选 ★                                │ │ │
│  │  │  • 并行/串行协调 ★                                      │ │ │
│  │  │  • 响应聚合与组织 ★                                     │ │ │
│  │  │  • 多轮对话管理                                          │ │ │
│  │  │  • A2UI 合并生成 ★                                      │ │ │
│  │  │                                                         │ │ │
│  │  └──────────────────────┬──────────────────────────────────┘ │ │
│  │                         │                                     │ │
│  │         ┌───────────────┴───────────────┐                    │ │
│  │         │    多 Agent 并行请求 ★          │                    │ │
│  │         │                                │                    │ │
│  │    ┌────▼────┐  ┌────▼────┐  ┌────▼────┐                    │ │
│  │    │美团 Agent│  │饿了么    │  │大众点评  │                    │ │
│  │    │         │  │ Agent   │  │ Agent   │                    │ │
│  │    └────┬────┘  └────┬────┘  └────┬────┘                    │ │
│  │         │            │            │                           │ │
│  │         └────────────┴────────────┘                           │ │
│  │                     │                                         │ │
│  │                     ▼                                         │ │
│  │         ┌──────────────────────┐                             │ │
│  │         │  响应聚合器 ★         │                             │ │
│  │         │  • 去重               │                             │ │
│  │         │  • 排序               │                             │ │
│  │         │  • 筛选               │                             │ │
│  │         │  • 合并 A2UI          │                             │ │
│  │         └──────────────────────┘                             │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                              │                                     │
│                              ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    A2UI 渲染引擎                               │ │
│  │              (解析 A2UI JSON，生成 NanoView)                    │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                              │                                     │
│                              ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    NanoView 视图层                             │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

★ 标记表示多 Agent 协作的关键组件
```

## 3. Agent 通信协议

### 3.1 Agent 消息格式

```kotlin
/**
 * Agent 间通信的消息
 */
@Serializable
data class AgentMessage(
    val messageId: String,              // 消息唯一ID
    val from: AgentIdentity,            // 发送方
    val to: AgentIdentity,              // 接收方
    val type: AgentMessageType,         // 消息类型
    val payload: AgentPayload,          // 消息内容
    val context: AgentContext,          // 上下文信息
    val timestamp: Long
)

@Serializable
data class AgentIdentity(
    val type: AgentType,                // SYSTEM / APP
    val id: String,                     // 唯一标识
    val name: String                    // 可读名称
)

enum class AgentType {
    SYSTEM,    // 系统 Agent
    APP        // 应用 Agent
}

enum class AgentMessageType {
    // 请求类型
    TASK_REQUEST,        // 任务请求
    QUERY_REQUEST,       // 查询请求
    ACTION_REQUEST,      // 动作请求

    // 响应类型
    TASK_RESPONSE,       // 任务响应
    QUERY_RESPONSE,      // 查询响应
    ACTION_RESPONSE,     // 动作响应

    // 状态类型
    STATUS_UPDATE,       // 状态更新
    ERROR,               // 错误

    // 协商类型
    CAPABILITY_QUERY,    // 能力查询
    CAPABILITY_RESPONSE  // 能力响应
}
```

### 3.2 请求 Payload

```kotlin
/**
 * System Agent → App Agent 的任务请求
 */
@Serializable
data class TaskRequestPayload(
    val intent: IntentInfo,             // 识别的意图
    val entities: Map<String, Any>,     // 提取的实体
    val userQuery: String,              // 用户原始输入
    val expectedResponseType: ResponseType,  // 期望的响应类型
    val constraints: TaskConstraints?   // 任务约束
) : AgentPayload()

@Serializable
data class IntentInfo(
    val type: String,           // 意图类型标识
    val action: String,         // 具体动作: "search", "order", "query"
    val confidence: Float       // 置信度
)

enum class ResponseType {
    RAW_DATA,          // 返回原始数据，由 System Agent 生成 A2UI
    A2UI_JSON,         // 返回 A2UI JSON，系统直接渲染
    HYBRID             // 返回数据 + 建议的 A2UI（System Agent 可修改）
}

@Serializable
data class TaskConstraints(
    val maxItems: Int? = null,          // 最大返回条目
    val timeout: Long? = null,          // 超时时间
    val requiredFields: List<String>? = null  // 必需字段
)
```

### 3.3 响应 Payload

```kotlin
/**
 * App Agent → System Agent 的任务响应
 */
@Serializable
data class TaskResponsePayload(
    val status: TaskStatus,
    val data: ResponseData?,            // 业务数据
    val a2ui: A2UISpec?,                // A2UI 渲染规格（可选）
    val message: String?,               // 给用户的消息
    val followUpActions: List<FollowUpAction>?  // 后续可用操作
) : AgentPayload()

enum class TaskStatus {
    SUCCESS,           // 成功
    PARTIAL,           // 部分成功
    NEED_MORE_INFO,    // 需要更多信息
    NEED_CONFIRMATION, // 需要用户确认
    FAILED,            // 失败
    IN_PROGRESS        // 处理中
}

@Serializable
data class ResponseData(
    val type: String,           // 数据类型: "product_list", "order", "message"
    val items: List<JsonElement>,  // 数据项
    val metadata: Map<String, Any>? = null
)

@Serializable
data class FollowUpAction(
    val id: String,
    val label: String,          // "加入购物车", "查看详情"
    val actionType: String,     // 动作类型
    val params: Map<String, Any>?
)
```

## 4. A2UI 协议规格

### 4.1 A2UI JSON 结构

```kotlin
/**
 * A2UI 渲染规格
 */
@Serializable
data class A2UISpec(
    val version: String = "1.0",
    val root: A2UIComponent
)

@Serializable
sealed class A2UIComponent {
    abstract val id: String?
    abstract val style: A2UIStyle?
}

/**
 * 容器组件
 */
@Serializable
data class A2UIContainer(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val direction: Direction = Direction.VERTICAL,
    val children: List<A2UIComponent>
) : A2UIComponent()

enum class Direction { VERTICAL, HORIZONTAL }

/**
 * 文本组件
 */
@Serializable
data class A2UIText(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val text: String,
    val textStyle: TextStyle? = null
) : A2UIComponent()

/**
 * 按钮组件
 */
@Serializable
data class A2UIButton(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val text: String,
    val action: A2UIAction
) : A2UIComponent()

/**
 * 图片组件
 */
@Serializable
data class A2UIImage(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val url: String,
    val alt: String? = null
) : A2UIComponent()

/**
 * 列表组件
 */
@Serializable
data class A2UIList(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val items: List<A2UIListItem>,
    val onItemClick: A2UIAction? = null
) : A2UIComponent()

@Serializable
data class A2UIListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val image: String? = null,
    val trailing: String? = null,       // 右侧文字（如价格）
    val data: Map<String, Any>? = null  // 附加数据
)

/**
 * 卡片组件
 */
@Serializable
data class A2UICard(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val header: A2UIComponent? = null,
    val content: A2UIComponent,
    val footer: A2UIComponent? = null,
    val action: A2UIAction? = null
) : A2UIComponent()

/**
 * 输入组件
 */
@Serializable
data class A2UIInput(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val placeholder: String? = null,
    val value: String? = null,
    val inputType: InputType = InputType.TEXT,
    val onSubmit: A2UIAction? = null
) : A2UIComponent()

enum class InputType { TEXT, NUMBER, PASSWORD, MULTILINE }
```

### 4.2 A2UI 样式与动作

```kotlin
/**
 * 样式定义
 */
@Serializable
data class A2UIStyle(
    val width: Dimension? = null,
    val height: Dimension? = null,
    val padding: Spacing? = null,
    val margin: Spacing? = null,
    val backgroundColor: String? = null,
    val borderRadius: Int? = null
)

@Serializable
sealed class Dimension {
    @Serializable data class Fixed(val value: Int) : Dimension()
    @Serializable data class Percent(val value: Float) : Dimension()
    @Serializable object WrapContent : Dimension()
    @Serializable object MatchParent : Dimension()
}

@Serializable
data class Spacing(
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0
)

@Serializable
data class TextStyle(
    val fontSize: Int? = null,
    val fontWeight: FontWeight? = null,
    val color: String? = null,
    val align: TextAlign? = null
)

enum class FontWeight { NORMAL, BOLD }
enum class TextAlign { LEFT, CENTER, RIGHT }

/**
 * 动作定义
 */
@Serializable
data class A2UIAction(
    val type: ActionType,
    val target: String,                 // 目标 Agent 或组件
    val method: String,                 // 方法名
    val params: Map<String, Any>? = null
)

enum class ActionType {
    AGENT_CALL,        // 调用 Agent
    NAVIGATE,          // 页面跳转
    SUBMIT,            // 提交表单
    DISMISS,           // 关闭
    CUSTOM             // 自定义
}
```

### 4.3 A2UI JSON 示例

```json
{
  "version": "1.0",
  "root": {
    "type": "container",
    "direction": "vertical",
    "style": { "padding": { "top": 16, "right": 16, "bottom": 16, "left": 16 } },
    "children": [
      {
        "type": "text",
        "text": "为您找到以下黄焖鸡店铺：",
        "textStyle": { "fontSize": 16, "fontWeight": "BOLD" }
      },
      {
        "type": "list",
        "items": [
          {
            "id": "shop_001",
            "title": "杨铭宇黄焖鸡米饭",
            "subtitle": "月售 1000+ | 配送费 ¥3",
            "image": "https://...",
            "trailing": "¥18起",
            "data": { "shopId": "001", "rating": 4.8 }
          },
          {
            "id": "shop_002",
            "title": "老王黄焖鸡",
            "subtitle": "月售 500+ | 配送费 ¥2",
            "image": "https://...",
            "trailing": "¥15起",
            "data": { "shopId": "002", "rating": 4.5 }
          }
        ],
        "onItemClick": {
          "type": "AGENT_CALL",
          "target": "meituan_agent",
          "method": "openShop",
          "params": { "shopId": "${item.data.shopId}" }
        }
      },
      {
        "type": "button",
        "text": "查看更多",
        "action": {
          "type": "AGENT_CALL",
          "target": "meituan_agent",
          "method": "loadMore",
          "params": { "page": 2 }
        }
      }
    ]
  }
}
```

## 5. System Agent 设计

### 5.1 核心设计——多 Agent 协作

System Agent 的核心能力是**一个意图触发多个 Agent 协同响应**，并将结果组织筛选后统一展示。

```
用户: "附近有什么好吃的？"
         │
         ▼
┌─────────────────────────────┐
│   System Agent: 意图理解     │
│   → 需要搜索附近餐厅         │
│   → 可用能力: SEARCH         │
│   → 匹配 Agent: 美团、饿了么  │
│                 大众点评      │
└─────────────┬───────────────┘
              │ 并行分发
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌────────┐ ┌────────┐ ┌────────┐
│美团    │ │饿了么  │ │大众点评│
│ Agent  │ │ Agent  │ │ Agent  │
│        │ │        │ │        │
│10结果  │ │8结果   │ │12结果  │
└───┬────┘ └───┬────┘ └───┬────┘
    └─────────┼─────────┘
              ▼
┌─────────────────────────────┐
│   响应聚合器 (Aggregator)    │
│   1. 收集所有响应            │
│   2. 去重（同一商品多平台）   │
│   3. 按评分/距离排序          │
│   4. 标注来源                │
│   5. 合并 A2UI 展示          │
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│   统一展示                   │
│   ┌─────────────────┐       │
│   │ 杨铭宇 ⭐4.8     │  美团 │
│   │ 老王鸡 ⭐4.5     │  多平 │
│   │ 辣妈辣妹 ⭐4.3   │  台   │
│   └─────────────────┘       │
└─────────────────────────────┘
```

### 5.2 核心实现

```kotlin
/**
 * System Agent - 系统智能体
 *
 * 核心能力:
 * 1. 意图理解
 * 2. 多 Agent 选择与筛选
 * 3. 并行/串行协调执行
 * 4. 响应聚合与组织
 * 5. A2UI 合并生成
 * 6. 对话上下文管理
 */
class SystemAgent(
    private val llmProvider: LLMProvider,
    private val agentRegistry: AgentRegistry,
    private val agentCoordinator: AgentCoordinator,
    private val responseAggregator: ResponseAggregator,
    private val a2uiGenerator: A2UIGenerator
) {

    /**
     * 处理用户输入 - 核心入口
     */
    suspend fun processUserInput(
        input: String,
        conversationContext: ConversationContext
    ): SystemAgentResponse {

        // 1. 意图理解 (含多 Agent 需求分析)
        val understanding = understandIntent(input, conversationContext)

        // 2. 选择所有匹配的 Agent
        val selectedAgents = selectAgents(understanding)

        // 3. 协调执行 (并行或串行)
        val responses = agentCoordinator.coordinate(
            agents = selectedAgents,
            understanding = understanding,
            context = conversationContext
        )

        // 4. 响应聚合
        val aggregated = responseAggregator.aggregate(
            responses = responses,
            understanding = understanding
        )

        // 5. 生成统一 UI
        return buildFinalResponse(aggregated, understanding)
    }

    /**
     * 选择匹配的 Agent
     *
     * 策略:
     * - 用户明确指定 app → 仅选对应 Agent
     * - 用户未指定 app → 根据能力匹配所有相关 Agent
     * - 系统操作 → 仅系统 Agent
     */
    private fun selectAgents(understanding: IntentUnderstanding): List<AppAgent> {
        return when {
            // 用户明确指定了应用
            understanding.targetApps.isNotEmpty() -> {
                understanding.targetApps.mapNotNull {
                    agentRegistry.getAgent(it)
                }
            }

            // 根据能力匹配多个 Agent
            understanding.requiredCapabilities.isNotEmpty() -> {
                agentRegistry.findAgentsByCapabilities(
                    understanding.requiredCapabilities
                )
            }

            // 系统级意图
            understanding.intentType.isSystemIntent() -> {
                listOfNotNull(agentRegistry.getSystemSettingsAgent())
            }

            // 兜底: 所有支持当前意图的 Agent
            else -> agentRegistry.findAgentsByIntent(understanding.intentType)
        }
    }

    /**
     * 构建最终响应
     * 处理多 Agent 响应的 A2UI 合并
     */
    private suspend fun buildFinalResponse(
        aggregated: AggregatedResponse,
        understanding: IntentUnderstanding
    ): SystemAgentResponse {

        val a2ui = when {
            // 所有 Agent 都提供了 A2UI → 合并展示
            aggregated.a2uiResponses.isNotEmpty() -> {
                a2uiGenerator.merge(
                    specs = aggregated.a2uiResponses,
                    layout = understanding.preferredLayout
                )
            }

            // 部分 Agent 返回原始数据 → System Agent 生成 A2UI
            aggregated.rawDataResponses.isNotEmpty() -> {
                a2uiGenerator.generateFromMultiple(
                    responses = aggregated.rawDataResponses,
                    understanding = understanding
                )
            }

            // 混合模式 → 合并 A2UI + 生成补充
            else -> {
                a2uiGenerator.mergeHybrid(
                    existingA2ui = aggregated.a2uiResponses,
                    rawData = aggregated.rawDataResponses,
                    understanding = understanding
                )
            }
        }

        return SystemAgentResponse(
            message = aggregated.summary,
            a2ui = a2ui,
            conversationState = aggregated.overallState,
            followUpSuggestions = aggregated.allFollowUpActions.map { it.label },
            participatingAgents = aggregated.participatingAgents
        )
    }
}
```

### 5.3 Agent 协调器

```kotlin
/**
 * Agent 协调器 - 负责多 Agent 的执行调度
 */
class AgentCoordinator(
    private val scope: CoroutineScope
) {

    /**
     * 协调多个 Agent 执行
     *
     * 执行策略:
     * - PARALLEL: 所有 Agent 同时执行 (默认, 用于搜索类任务)
     * - SEQUENTIAL: 依次执行 (用于有依赖的任务链)
     * - RACE: 最快响应赢 (用于冗余查询)
     * - FALLBACK: 前一个失败才执行下一个
     */
    suspend fun coordinate(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {

        val strategy = understanding.coordinationStrategy

        return when (strategy) {
            CoordinationStrategy.PARALLEL -> executeParallel(agents, understanding, context)
            CoordinationStrategy.SEQUENTIAL -> executeSequential(agents, understanding, context)
            CoordinationStrategy.RACE -> executeRace(agents, understanding, context)
            CoordinationStrategy.FALLBACK -> executeFallback(agents, understanding, context)
        }
    }

    /**
     * 并行执行 - 同时请求所有 Agent，等待全部响应
     * 适用于: "附近有什么好吃的" → 美团 + 饿了么 同时搜索
     */
    private suspend fun executeParallel(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        val timeout = understanding.timeout ?: DEFAULT_PARALLEL_TIMEOUT

        return withTimeout(timeout) {
            agents.map { agent ->
                async {
                    try {
                        val request = buildRequest(agent, understanding, context)
                        val response = agent.handleRequest(request)
                        AgentResponse(agent = agent, payload = response, success = true)
                    } catch (e: Exception) {
                        AgentResponse(agent = agent, error = e, success = false)
                    }
                }
            }.map { it.await() }
        }
    }

    /**
     * 串行执行 - 依次执行，后续可用前面的结果
     * 适用于: "打开美团" → "搜索黄焖鸡" → "选择第一家"
     */
    private suspend fun executeSequential(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        val results = mutableListOf<AgentResponse>()
        var currentContext = context

        for (agent in agents) {
            val request = buildRequest(agent, understanding, currentContext)
            val response = agent.handleRequest(request)
            val result = AgentResponse(agent = agent, payload = response, success = true)
            results.add(result)

            // 更新上下文，供下一个 Agent 使用
            currentContext = currentContext.withPreviousResult(result)
        }
        return results
    }

    /**
     * 赛跑模式 - 取最快响应
     * 适用于: 多个 LLM Provider 互相备用
     */
    private suspend fun executeRace(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        val timeout = understanding.timeout ?: DEFAULT_RACE_TIMEOUT
        var winner: AgentResponse? = null

        withTimeout(timeout) {
            val channel = Channel<AgentResponse>(agents.size)
            agents.forEach { agent ->
                launch {
                    try {
                        val request = buildRequest(agent, understanding, context)
                        val response = agent.handleRequest(request)
                        channel.send(AgentResponse(agent, response, true))
                    } catch (e: Exception) {
                        channel.send(AgentResponse(agent, error = e, success = false))
                    }
                }
            }
            // 等待第一个成功响应
            repeat(agents.size) {
                val result = channel.receive()
                if (result.success && winner == null) {
                    winner = result
                }
            }
        }
        return listOfNotNull(winner)
    }

    /**
     * 回退模式 - 前一个失败才尝试下一个
     * 适用于: 首选平台不可用时切换到备用平台
     */
    private suspend fun executeFallback(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        for (agent in agents) {
            try {
                val request = buildRequest(agent, understanding, context)
                val response = agent.handleRequest(request)
                return listOf(AgentResponse(agent, response, true))
            } catch (e: Exception) {
                NanoLog.w(TAG, "Agent ${agent.agentId} failed, trying next...")
                continue
            }
        }
        return emptyList()
    }

    companion object {
        const val DEFAULT_PARALLEL_TIMEOUT = 10000L  // 10s
        const val DEFAULT_RACE_TIMEOUT = 5000L       // 5s
    }
}

/**
 * 协调策略
 */
enum class CoordinationStrategy {
    PARALLEL,      // 并行: 同时请求多个 Agent
    SEQUENTIAL,    // 串行: 依次执行，后续可用前面结果
    RACE,          // 赛跑: 取最快响应
    FALLBACK       // 回退: 前一个失败才执行下一个
}
```

### 5.4 响应聚合器

```kotlin
/**
 * 响应聚合器 - 合并多个 Agent 的响应
 *
 * 核心能力:
 * 1. 去重: 同一商品在多个平台出现时去重
 * 2. 排序: 按评分、距离等维度排序
 * 3. 筛选: 根据约束条件过滤
 * 4. 分组: 按来源 Agent 分组或按类别分组
 * 5. 合并 A2UI: 将多个 A2UI 组织成一个统一界面
 */
class ResponseAggregator(
    private val llmProvider: LLMProvider
) {

    /**
     * 聚合多个 Agent 的响应
     */
    suspend fun aggregate(
        responses: List<AgentResponse>,
        understanding: IntentUnderstanding
    ): AggregatedResponse {

        val successResponses = responses.filter { it.success }

        // 分类响应
        val a2uiResponses = successResponses
            .filter { it.payload.a2ui != null }
            .map { it.payload.a2ui!! to it.agent.agentId }

        val rawDataResponses = successResponses
            .filter { it.payload.data != null && it.payload.a2ui == null }
            .map { it.payload.data!! to it.agent.agentId }

        // 合并数据
        val mergedData = mergeData(rawDataResponses, understanding)

        // 生成摘要
        val summary = generateSummary(successResponses, understanding)

        // 合并后续操作
        val allFollowUpActions = successResponses
            .flatMap { it.payload.followUpActions ?: emptyList() }
            .map { it.withAgentSource(it.id) }

        return AggregatedResponse(
            a2uiResponses = a2uiResponses,
            rawDataResponses = rawDataResponses,
            mergedData = mergedData,
            summary = summary,
            allFollowUpActions = allFollowUpActions,
            participatingAgents = successResponses.map { it.agent.agentId },
            overallState = determineOverallState(successResponses),
            failedAgents = responses.filter { !it.success }.map { it.agent.agentId }
        )
    }

    /**
     * 合并来自多个 Agent 的同类数据
     *
     * 示例: 美团返回 [黄焖鸡A, 黄焖鸡B]
     *        饿了么返回 [黄焖鸡C, 黄焖鸡A(同店)]
     *
     * 合并结果: [黄焖鸡A(美团+饿了么), 黄焖鸡B(美团), 黄焖鸡C(饿了么)]
     *
     * 每个项目带有来源标注，用户可知道哪个平台
     */
    private fun mergeData(
        rawResponses: List<Pair<ResponseData, String>>,  // data + agentId
        understanding: IntentUnderstanding
    ): MergedResponseData {

        // 按数据 key 分组 (如: shop_name)
        val allItems = rawResponses.flatMap { (data, agentId) ->
            data.items.map { item ->
                MergedItem(
                    data = item,
                    source = agentId,
                    key = extractDedupeKey(item, understanding)
                )
            }
        }

        // 去重: 相同 key 的合并到一个条目
        val deduped = allItems
            .groupBy { it.key }
            .map { (_, items) ->
                MergedItem(
                    data = items.first().data,
                    sources = items.map { it.source },
                    key = items.first().key,
                    allVariants = items  // 保留所有平台版本
                )
            }

        // 排序
        val sorted = sortItems(deduped, understanding.sortPreference)

        return MergedResponseData(
            items = sorted,
            totalCount = allItems.size,
            uniqueCount = deduped.size,
            sources = rawResponses.map { it.second }
        )
    }

    /**
     * 生成摘要文本 (用于展示给用户)
     */
    private suspend fun generateSummary(
        responses: List<AgentResponse>,
        understanding: IntentUnderstanding
    ): String {
        val sources = responses.map { it.agent.agentName }
        val totalItems = responses.sumOf { it.payload.data?.items?.size ?: 0 }

        return when {
            sources.size > 1 ->
                "已从 ${sources.joinToString("、")} 共找到 $totalItems 个结果"
            sources.size == 1 ->
                responses.first().payload.message
                    ?: "已找到 $totalItems 个结果"
            else -> "抱歉，没有找到相关结果"
        }
    }
}

/**
 * 聚合响应
 */
data class AggregatedResponse(
    val a2uiResponses: List<Pair<A2UISpec, String>>,    // A2UI + agentId
    val rawDataResponses: List<Pair<ResponseData, String>>,
    val mergedData: MergedResponseData?,
    val summary: String,
    val allFollowUpActions: List<FollowUpAction>,
    val participatingAgents: List<String>,
    val overallState: ConversationState,
    val failedAgents: List<String>
)

/**
 * 合并后的数据
 */
data class MergedResponseData(
    val items: List<MergedItem>,
    val totalCount: Int,
    val uniqueCount: Int,
    val sources: List<String>
)

data class MergedItem(
    val data: JsonElement,
    val source: String? = null,         // 单一来源
    val sources: List<String> = emptyList(),  // 多来源
    val key: String,                    // 去重键
    val allVariants: List<MergedItem> = emptyList()  // 多平台版本
)
```

### 5.5 A2UI 合并策略

```kotlin
/**
 * A2UI 生成器 - 支持合并多个来源的 A2UI
 */
class A2UIGenerator(
    private val llmProvider: LLMProvider
) {

    /**
     * 合并多个 Agent 各自生成的 A2UI
     *
     * 布局策略:
     * - TABS: 每个 Agent 一个 Tab (美团 | 饿了么 | 大众点评)
     * - UNIFIED_LIST: 合并为一个列表，每项带来源标签
     * - CARDS: 每个 Agent 的结果作为一张卡片
     */
    fun merge(
        specs: List<Pair<A2UISpec, String>>,  // A2UI + agentId
        layout: MergeLayout
    ): A2UISpec {
        return when (layout) {
            MergeLayout.TABS -> mergeTabs(specs)
            MergeLayout.UNIFIED_LIST -> mergeUnifiedList(specs)
            MergeLayout.CARDS -> mergeCards(specs)
        }
    }

    /**
     * Tab 布局: 每个 Agent 的结果独立展示
     * 适用于: 用户想对比不同平台
     *
     * ┌─────────────────────────────┐
     * │ 美团 ▼  饿了么    大众点评   │  ← Tab 选择
     * ├─────────────────────────────┤
     * │ 杨铭宇 ⭐4.8  ¥18起         │
     * │ 老王鸡 ⭐4.5  ¥15起         │  ← 当前 Tab 内容
     * │ ...                          │
     * └─────────────────────────────┘
     */
    private fun mergeTabs(specs: List<Pair<A2UISpec, String>>): A2UISpec {
        return A2UISpec(
            root = A2UIContainer(
                direction = Direction.VERTICAL,
                children = listOf(
                    // Tab 导航栏
                    A2UITabBar(
                        tabs = specs.map { (_, agentId) ->
                            A2UITab(id = agentId, label = resolveAgentName(agentId))
                        }
                    ),
                    // Tab 内容区
                    A2UITabContent(
                        tabs = specs.map { (spec, agentId) ->
                            A2UITab(id = agentId, label = resolveAgentName(agentId),
                                content = spec.root)
                        }
                    )
                )
            )
        )
    }

    /**
     * 统一列表: 所有 Agent 的结果合并为一个列表，带来源标签
     * 适用于: 用户想看所有选项
     *
     * ┌─────────────────────────────┐
     * │ 杨铭宇 ⭐4.8  ¥18  [美团]   │  ← 来源标签
     * │ 辣妈辣妹 ⭐4.3 ¥22  [饿了么] │
     * │ 老王鸡 ⭐4.5  ¥15  [多平台]  │  ← 多平台可用
     * └─────────────────────────────┘
     */
    private fun mergeUnifiedList(specs: List<Pair<A2UISpec, String>>): A2UISpec {
        // 从所有 A2UI 中提取列表项，合并排序
        val allItems = specs.flatMap { (spec, agentId) ->
            extractListItems(spec).map { item ->
                item.copy(trailing = "${item.trailing} [$agentId]")
            }
        }.sortedByDescending { it.data?.get("rating") as? Float ?: 0f }

        return A2UISpec(
            root = A2UIContainer(
                direction = Direction.VERTICAL,
                children = listOf(
                    A2UIText(
                        text = "综合推荐 (${allItems.size}家)",
                        textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD)
                    ),
                    A2UIList(items = allItems)
                )
            )
        )
    }

    /**
     * 卡片布局: 每个 Agent 的结果作为独立卡片
     * 适用于: 不同类型信息的组合展示
     *
     * ┌─────────────────────────────┐
     * │ 🍚 美团外卖                  │
     * │ 黄焖鸡 15家，最近10分钟      │ ← Agent 卡片 1
     * ├─────────────────────────────┤
     * │ 🗺️ 大众点评                  │
     * │ 黄焖鸡 22家，步行可达        │ ← Agent 卡片 2
     * └─────────────────────────────┘
     */
    private fun mergeCards(specs: List<Pair<A2UISpec, String>>): A2UISpec {
        return A2UISpec(
            root = A2UIContainer(
                direction = Direction.VERTICAL,
                children = specs.map { (spec, agentId) ->
                    A2UICard(
                        header = A2UIText(
                            text = resolveAgentName(agentId),
                            textStyle = TextStyle(fontSize = 16, fontWeight = FontWeight.BOLD)
                        ),
                        content = spec.root,
                        style = A2UIStyle(
                            margin = Spacing(bottom = 12),
                            borderRadius = 8
                        )
                    )
                }
            )
        )
    }

    /**
     * 从原始数据生成 A2UI (多数据源)
     * System Agent 代替 App Agent 生成 UI
     */
    suspend fun generateFromMultiple(
        responses: List<Pair<ResponseData, String>>,
        understanding: IntentUnderstanding
    ): A2UISpec {
        val prompt = buildMultiDataA2UIPrompt(responses, understanding)
        val llmResponse = llmProvider.chat(prompt)
        return parseA2UIResponse(llmResponse)
    }
}

/**
 * 合并布局策略
 */
enum class MergeLayout {
    TABS,           // Tab 切换
    UNIFIED_LIST,   // 统一列表
    CARDS           // 卡片列表
}
```

### 5.6 意图理解 Prompt (含多 Agent 分析)

```kotlin
object SystemAgentPrompts {

    val INTENT_UNDERSTANDING = """
你是 NanoAndroid 系统的智能助手。分析用户输入，判断需要哪些 Agent 参与。

## 当前可用 Agent
${agentCapabilities}  // 动态注入已注册 Agent 的能力描述

## 输出格式 (JSON)
{
  "intent_type": "意图类型",
  "target_apps": ["指定的 app (未指定时为空数组)"],
  "broadcast_capability": "未指定 app 时按能力广播的类型",
  "action": "具体动作",
  "entities": { "实体名": "实体值" },
  "confidence": 0.0-1.0,
  "coordination_strategy": "PARALLEL|SEQUENTIAL|RACE|FALLBACK",
  "merge_layout": "TABS|UNIFIED_LIST|CARDS",
  "needs_clarification": false,
  "clarification_question": null
}

## 协调策略选择规则
- 用户要对比多平台 → PARALLEL + TABS
- 用户想看所有选项 → PARALLEL + UNIFIED_LIST
- 有明确任务链依赖 → SEQUENTIAL
- 有备用平台 → FALLBACK
- 需要最快响应 → RACE

## 示例 1: 未指定平台 (广播)
输入: "附近有什么好吃的？"
输出: {
  "intent_type": "APP_SEARCH",
  "target_apps": [],
  "broadcast_capability": "SEARCH",
  "action": "search_nearby",
  "entities": { "location": "nearby", "category": "food" },
  "confidence": 0.9,
  "coordination_strategy": "PARALLEL",
  "merge_layout": "UNIFIED_LIST"
}

## 示例 2: 明确指定平台
输入: "帮我在美团上点一份黄焖鸡"
输出: {
  "intent_type": "APP_ORDER",
  "target_apps": ["meituan"],
  "action": "search_and_order",
  "entities": { "food_item": "黄焖鸡" },
  "confidence": 0.95,
  "coordination_strategy": "PARALLEL",
  "merge_layout": "UNIFIED_LIST"
}

## 示例 3: 对比需求
输入: "美团和饿了么哪家黄焖鸡便宜？"
输出: {
  "intent_type": "APP_SEARCH",
  "target_apps": ["meituan", "eleme"],
  "action": "search_compare",
  "entities": { "food_item": "黄焖鸡", "compare_by": "price" },
  "confidence": 0.92,
  "coordination_strategy": "PARALLEL",
  "merge_layout": "TABS"
}
""".trimIndent()

    val A2UI_GENERATION = """
根据以下多个来源的数据，生成一个统一的 A2UI JSON 展示。

## 数据来源:
${dataSourcesJson}  // 多个 Agent 的数据

## 上下文:
- 用户查询: {user_query}
- 意图: {intent}
- 期望布局: {layout}

## 要求:
1. 将多源数据组织为统一界面
2. 每项标注来源
3. 去重处理 (相同内容合并)
4. 按相关度排序
5. 包含交互动作

请直接输出 A2UI JSON。
""".trimIndent()
}
```

## 6. App Agent 设计

### 6.1 App Agent 接口

```kotlin
/**
 * App Agent 接口 - 应用需要实现此接口以支持 AI 交互
 */
interface AppAgent {

    /** Agent 标识 */
    val agentId: String

    /** Agent 名称 */
    val agentName: String

    /** 支持的能力 */
    val capabilities: Set<AgentCapability>

    /**
     * 处理来自 System Agent 的请求
     */
    suspend fun handleRequest(request: AgentMessage): TaskResponsePayload

    /**
     * 获取 Agent 能力描述（用于 System Agent 路由决策）
     */
    fun describeCapabilities(): AgentCapabilityDescription

    /**
     * 处理用户在 A2UI 上的交互
     */
    suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload
}

/**
 * Agent 能力
 */
enum class AgentCapability {
    SEARCH,          // 搜索
    ORDER,           // 下单
    PAYMENT,         // 支付
    MESSAGE,         // 消息
    NAVIGATION,      // 导航
    MEDIA,           // 媒体播放
    SETTINGS         // 设置
}

/**
 * 能力描述 - 帮助 System Agent 理解 App Agent 能做什么
 */
data class AgentCapabilityDescription(
    val agentId: String,
    val supportedIntents: List<String>,
    val supportedEntities: List<String>,
    val exampleQueries: List<String>,     // 示例查询
    val responseTypes: Set<ResponseType>  // 支持的响应类型
)
```

### 6.2 App Agent 基类

```kotlin
/**
 * App Agent 基类 - 提供通用实现
 */
abstract class BaseAppAgent(
    override val agentId: String,
    override val agentName: String,
    protected val llmProvider: LLMProvider? = null  // 可选的 LLM 支持
) : AppAgent {

    override suspend fun handleRequest(request: AgentMessage): TaskResponsePayload {
        return when (request.type) {
            AgentMessageType.TASK_REQUEST -> handleTaskRequest(
                request.payload as TaskRequestPayload
            )
            AgentMessageType.QUERY_REQUEST -> handleQueryRequest(
                request.payload as QueryRequestPayload
            )
            AgentMessageType.ACTION_REQUEST -> handleActionRequest(
                request.payload as ActionRequestPayload
            )
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "Unsupported message type: ${request.type}"
            )
        }
    }

    /**
     * 处理任务请求 - 子类实现具体业务逻辑
     */
    protected abstract suspend fun handleTaskRequest(
        request: TaskRequestPayload
    ): TaskResponsePayload

    /**
     * 处理查询请求
     */
    protected open suspend fun handleQueryRequest(
        request: QueryRequestPayload
    ): TaskResponsePayload {
        return TaskResponsePayload(
            status = TaskStatus.FAILED,
            message = "Query not supported"
        )
    }

    /**
     * 处理动作请求
     */
    protected open suspend fun handleActionRequest(
        request: ActionRequestPayload
    ): TaskResponsePayload {
        return TaskResponsePayload(
            status = TaskStatus.FAILED,
            message = "Action not supported"
        )
    }

    /**
     * 生成 A2UI - 使用 LLM（可选）
     */
    protected suspend fun generateA2UI(
        data: ResponseData,
        template: String? = null
    ): A2UISpec? {
        if (llmProvider == null) return null

        val prompt = buildA2UIPrompt(data, template)
        val response = llmProvider.chat(prompt)
        return parseA2UIResponse(response)
    }
}
```

### 6.3 美团 Agent 示例实现

```kotlin
/**
 * 美团外卖 App Agent
 */
class MeituanAgent(
    private val meituanApi: MeituanApi,
    llmProvider: LLMProvider? = null
) : BaseAppAgent(
    agentId = "meituan",
    agentName = "美团外卖",
    llmProvider = llmProvider
) {

    override val capabilities = setOf(
        AgentCapability.SEARCH,
        AgentCapability.ORDER
    )

    override fun describeCapabilities() = AgentCapabilityDescription(
        agentId = agentId,
        supportedIntents = listOf("APP_SEARCH", "APP_ORDER"),
        supportedEntities = listOf("food_item", "shop_name", "location"),
        exampleQueries = listOf(
            "搜索黄焖鸡",
            "点一份麻辣烫",
            "附近有什么好吃的"
        ),
        responseTypes = setOf(ResponseType.A2UI_JSON, ResponseType.RAW_DATA)
    )

    override suspend fun handleTaskRequest(
        request: TaskRequestPayload
    ): TaskResponsePayload {

        return when (request.intent.action) {
            "search", "search_and_order" -> handleSearch(request)
            "open_shop" -> handleOpenShop(request)
            "add_to_cart" -> handleAddToCart(request)
            "checkout" -> handleCheckout(request)
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "不支持的操作: ${request.intent.action}"
            )
        }
    }

    private suspend fun handleSearch(
        request: TaskRequestPayload
    ): TaskResponsePayload {
        val keyword = request.entities["food_item"] as? String
            ?: return TaskResponsePayload(
                status = TaskStatus.NEED_MORE_INFO,
                message = "你想吃什么呢？"
            )

        // 调用美团 API 搜索
        val searchResult = meituanApi.searchShops(keyword)

        // 构建响应数据
        val responseData = ResponseData(
            type = "shop_list",
            items = searchResult.shops.map { it.toJsonElement() },
            metadata = mapOf(
                "total" to searchResult.total,
                "keyword" to keyword
            )
        )

        // 生成 A2UI (App Agent 模式)
        val a2ui = buildShopListA2UI(searchResult.shops, keyword)

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            data = responseData,
            a2ui = a2ui,
            message = "为您找到 ${searchResult.total} 家「${keyword}」店铺",
            followUpActions = listOf(
                FollowUpAction("filter_rating", "按评分筛选", "filter", mapOf("by" to "rating")),
                FollowUpAction("filter_distance", "按距离筛选", "filter", mapOf("by" to "distance"))
            )
        )
    }

    /**
     * 构建店铺列表的 A2UI
     */
    private fun buildShopListA2UI(shops: List<Shop>, keyword: String): A2UISpec {
        return A2UISpec(
            root = A2UIContainer(
                direction = Direction.VERTICAL,
                style = A2UIStyle(padding = Spacing(16, 16, 16, 16)),
                children = listOf(
                    A2UIText(
                        text = "「${keyword}」搜索结果",
                        textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD)
                    ),
                    A2UIList(
                        items = shops.map { shop ->
                            A2UIListItem(
                                id = shop.id,
                                title = shop.name,
                                subtitle = "月售${shop.monthlySales} | ${shop.deliveryTime}分钟",
                                image = shop.logo,
                                trailing = "¥${shop.minPrice}起",
                                data = mapOf("shopId" to shop.id, "rating" to shop.rating)
                            )
                        },
                        onItemClick = A2UIAction(
                            type = ActionType.AGENT_CALL,
                            target = "meituan",
                            method = "openShop",
                            params = mapOf("shopId" to "\${item.data.shopId}")
                        )
                    )
                )
            )
        )
    }

    override suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload {
        return when (action.method) {
            "openShop" -> {
                val shopId = action.params?.get("shopId") as? String
                    ?: return TaskResponsePayload(status = TaskStatus.FAILED, message = "缺少店铺ID")
                handleOpenShop(shopId)
            }
            "addToCart" -> {
                val itemId = action.params?.get("itemId") as? String
                    ?: return TaskResponsePayload(status = TaskStatus.FAILED, message = "缺少商品ID")
                handleAddToCart(itemId)
            }
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "未知操作: ${action.method}"
            )
        }
    }
}
```

## 7. Agent 注册与发现

```kotlin
/**
 * Agent 注册表 - 管理所有可用的 App Agent
 */
class AgentRegistry {

    private val agents = ConcurrentHashMap<String, AppAgent>()
    private val capabilityIndex = mutableMapOf<AgentCapability, MutableList<String>>()

    /**
     * 注册 Agent
     */
    fun registerAgent(agent: AppAgent) {
        agents[agent.agentId] = agent

        // 建立能力索引
        agent.capabilities.forEach { capability ->
            capabilityIndex.getOrPut(capability) { mutableListOf() }
                .add(agent.agentId)
        }

        NanoLog.i(TAG, "Registered agent: ${agent.agentId} (${agent.agentName})")
    }

    /**
     * 注销 Agent
     */
    fun unregisterAgent(agentId: String) {
        val agent = agents.remove(agentId) ?: return

        agent.capabilities.forEach { capability ->
            capabilityIndex[capability]?.remove(agentId)
        }
    }

    /**
     * 获取 Agent
     */
    fun getAgent(agentId: String): AppAgent? = agents[agentId]

    /**
     * 根据能力查找 Agent
     */
    fun findAgentsByCapability(capability: AgentCapability): List<AppAgent> {
        return capabilityIndex[capability]?.mapNotNull { agents[it] } ?: emptyList()
    }

    /**
     * 获取所有 Agent 的能力描述
     */
    fun getAllCapabilities(): List<AgentCapabilityDescription> {
        return agents.values.map { it.describeCapabilities() }
    }

    /**
     * 系统设置 Agent
     */
    fun getSystemSettingsAgent(): AppAgent? = agents["system_settings"]

    /**
     * 默认 Agent（处理通用请求）
     */
    fun getDefaultAgent(): AppAgent? = agents["default"]
}
```

## 8. 完整交互流程示例

### 8.1 场景一：单 Agent 定向请求

用户明确指定了目标平台，System Agent 仅路由给单个 App Agent。

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户: "帮我在美团点一份黄焖鸡"                                             │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ System Agent: 意图理解                                                    │
│                                                                          │
│ LLM 分析结果:                                                             │
│ {                                                                        │
│   "intent_type": "APP_ORDER",                                            │
│   "target_apps": ["meituan"],                                            │
│   "action": "search_and_order",                                          │
│   "entities": { "food_item": "黄焖鸡" },                                  │
│   "confidence": 0.95,                                                    │
│   "coordination_strategy": "PARALLEL",                                   │
│   "merge_layout": "UNIFIED_LIST"                                         │
│ }                                                                        │
│                                                                          │
│ → 仅匹配到 meituan Agent，单 Agent 模式                                   │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ AgentCoordinator: PARALLEL 执行 [meituan]                                │
│                                                                          │
│ System Agent → Meituan Agent: 发送请求                                    │
│ AgentMessage {                                                           │
│   from: { type: SYSTEM, id: "system_agent" },                            │
│   to: { type: APP, id: "meituan" },                                      │
│   type: TASK_REQUEST,                                                    │
│   payload: {                                                             │
│     intent: { type: "APP_ORDER", action: "search_and_order" },           │
│     entities: { "food_item": "黄焖鸡" },                                  │
│     expectedResponseType: "A2UI_JSON"                                    │
│   }                                                                      │
│ }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ Meituan Agent 处理 → 返回 A2UI_JSON 模式响应                              │
│                                                                          │
│ TaskResponsePayload {                                                    │
│   status: SUCCESS,                                                       │
│   message: "为您找到 15 家「黄焖鸡」店铺",                                  │
│   data: { type: "shop_list", items: [...15个店铺...] },                   │
│   a2ui: { ...完整的 A2UI JSON... },                                      │
│   followUpActions: ["按评分筛选", "按距离筛选"]                             │
│ }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ ResponseAggregator: 单 Agent 结果，直接透传                               │
│ System Agent → A2UI 渲染引擎 → NanoView 展示                             │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户界面:                                                                 │
│ ┌────────────────────────────────────────────────────────────────────┐  │
│ │ 为您找到 15 家「黄焖鸡」店铺                                         │  │
│ ├────────────────────────────────────────────────────────────────────┤  │
│ │ 杨铭宇黄焖鸡米饭  ⭐4.8 | 月售1000+           ¥18起                 │  │
│ │ 老王黄焖鸡        ⭐4.5 | 月售500+            ¥15起                 │  │
│ │ ...                                                                │  │
│ │ [按评分筛选] [按距离筛选] [查看更多]                                 │  │
│ └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ 用户点击店铺
┌──────────────────────────────────────────────────────────────────────────┐
│ A2UI 触发动作 → System Agent 路由回 Meituan Agent                         │
│ { type: "AGENT_CALL", target: "meituan", method: "openShop" }            │
│ → 继续与 Meituan Agent 交互，展示店铺详情和菜品                            │
└──────────────────────────────────────────────────────────────────────────┘
```

### 8.2 场景二：多 Agent 广播并聚合（核心场景）

用户未指定平台，System Agent 广播给所有匹配的 Agent，收集并聚合响应。

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户: "附近有什么好吃的？"                                                │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ System Agent: 意图理解 + 多 Agent 选择                                    │
│                                                                          │
│ {                                                                        │
│   "intent_type": "APP_SEARCH",                                           │
│   "target_apps": [],              ← 用户未指定，广播                      │
│   "broadcast_capability": "SEARCH",                                      │
│   "action": "search_nearby",                                             │
│   "entities": { "location": "nearby", "category": "food" },             │
│   "coordination_strategy": "PARALLEL",                                   │
│   "merge_layout": "UNIFIED_LIST"  ← 合并为统一列表                       │
│ }                                                                        │
│                                                                          │
│ AgentRegistry.findAgentsByCapability(SEARCH):                            │
│   → [meituan_agent, eleme_agent, dianping_agent]                         │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                      ┌─────────────┼─────────────┐
                      ▼             ▼             ▼
┌────────────────┐ ┌────────────┐ ┌────────────────┐
│ AgentCoordinator: PARALLEL 同时分发给 3 个 Agent              │
│                                                                │
│ Meituan Agent  │ Eleme Agent  │ Dianping Agent  │
│ request:       │ request:     │ request:        │
│ search_nearby  │ search_nearby│ search_nearby   │
│ "food"         │ "food"       │ "food"          │
└───┬────────────┘ └──┬─────────┘ └───┬────────────┘
    │                 │               │
    ▼                 ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────────┐
│ 美团返回:   │ 饿了么返回:  │ 大众点评返回:    │
│ 10家 A2UI  │ 8家 RAW_DATA │ 12家 A2UI      │
│ (模式A)    │ (模式B)      │ (模式A)        │
└───┬────────┘ └──┬─────────┘ └───┬────────────┘
    │             │               │
    └─────────────┼───────────────┘
                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ ResponseAggregator: 多源聚合                                             │
│                                                                          │
│ 步骤 1: 分类响应                                                         │
│   a2uiResponses:    [(meituan, A2UISpec), (dianping, A2UISpec)]          │
│   rawDataResponses: [(eleme, ResponseData)]                              │
│                                                                          │
│ 步骤 2: 去重检测                                                         │
│   美团 "杨铭宇黄焖鸡" == 饿了么 "杨铭宇黄焖鸡" (同店铺)                     │
│   → 合并为 MergedItem { sources: ["meituan", "eleme"] }                  │
│                                                                          │
│ 步骤 3: 排序                                                             │
│   按评分 desc → [杨铭宇 4.8, 辣妈辣妹 4.6, 老王鸡 4.5, ...]              │
│                                                                          │
│ 步骤 4: 生成摘要                                                         │
│   "已从美团、饿了么、大众点评共找到 25 个结果 (去重后 18 家)"              │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ A2UIGenerator: 混合模式合并 (mergeHybrid)                                │
│                                                                          │
│ ① 美团 A2UI + 大众点评 A2UI → 提取列表项                                │
│ ② 饿了么 RAW_DATA → System Agent 调用 LLM 生成 A2UI                     │
│ ③ 所有项目合并到 UNIFIED_LIST 布局                                       │
│ ④ 每项添加来源标签 [美团] / [饿了么] / [多平台]                           │
│                                                                          │
│ 最终 A2UISpec:                                                           │
│ Container(vertical) {                                                    │
│   Text("已从美团、饿了么、大众点评共找到 18 家")                           │
│   List [                                                                 │
│     { "杨铭宇", ⭐4.8, ¥18起, [多平台] },  ← 去重合并                    │
│     { "辣妈辣妹", ⭐4.6, ¥22起, [饿了么] },                             │
│     { "老王鸡", ⭐4.5, ¥15起, [美团] },                                 │
│     ...                                                                  │
│   ]                                                                      │
│   ButtonGroup(["按评分", "按距离", "按价格", "查看更多"])                  │
│ }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户界面 (UNIFIED_LIST 布局):                                            │
│ ┌────────────────────────────────────────────────────────────────────┐  │
│ │ 已从美团、饿了么、大众点评共找到 18 家                                 │  │
│ ├────────────────────────────────────────────────────────────────────┤  │
│ │ 杨铭宇黄焖鸡米饭  ⭐4.8  ¥18起  [多平台]                            │  │
│ │ 辣妈辣妹         ⭐4.6  ¥22起  [饿了么]                            │  │
│ │ 老王黄焖鸡       ⭐4.5  ¥15起  [美团]                              │  │
│ │ ...                                                                │  │
│ │ [按评分] [按距离] [按价格] [查看更多]                                │  │
│ └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ 用户点击 "杨铭宇" (多平台)
┌──────────────────────────────────────────────────────────────────────────┐
│ A2UI 触发 → System Agent 需要选择平台                                    │
│                                                                          │
│ System Agent 生成确认 UI:                                                │
│ ┌──────────────────────────────┐                                        │
│ │ 该店铺在以下平台可用:         │                                        │
│ │ [美团 ¥18起] [饿了么 ¥19起]   │  ← 多平台选择                         │
│ └──────────────────────────────┘                                        │
│                                                                          │
│ 用户选择平台后 → 路由给对应 Agent 继续交互                                │
└──────────────────────────────────────────────────────────────────────────┘
```

### 8.3 场景三：对比需求 (TABS 布局)

用户明确要求比较多个平台，使用 TABS 布局独立展示各平台结果。

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户: "美团和饿了么哪家黄焖鸡便宜？"                                      │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ System Agent: 意图理解                                                    │
│                                                                          │
│ {                                                                        │
│   "intent_type": "APP_SEARCH",                                           │
│   "target_apps": ["meituan", "eleme"],  ← 明确指定两个平台                │
│   "action": "search_compare",                                            │
│   "entities": { "food_item": "黄焖鸡", "compare_by": "price" },         │
│   "coordination_strategy": "PARALLEL",                                   │
│   "merge_layout": "TABS"  ← 对比需求，使用 Tab 布局                      │
│ }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                      ┌─────────────┴─────────────┐
                      ▼                           ▼
┌────────────────────────────┐   ┌────────────────────────────┐
│ Meituan Agent              │   │ Eleme Agent                │
│ 搜索"黄焖鸡"               │   │ 搜索"黄焖鸡"               │
│ → 返回 A2UI (价格排序)      │   │ → 返回 A2UI (价格排序)      │
└──────────┬─────────────────┘   └──────────┬─────────────────┘
           └─────────────┬───────────────────┘
                         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ A2UIGenerator.mergeTabs():                                               │
│                                                                          │
│ A2UISpec {                                                               │
│   TabBar(["美团", "饿了么"]),                                             │
│   TabContent {                                                           │
│     Tab("美团"): Meituan 的 A2UISpec,                                    │
│     Tab("饿了么"): Eleme 的 A2UISpec                                     │
│   }                                                                      │
│ }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 用户界面 (TABS 布局):                                                    │
│ ┌────────────────────────────────────────────────────────────────────┐  │
│ │ [美团 ▼]        饿了么                                              │  │
│ ├────────────────────────────────────────────────────────────────────┤  │
│ │ 老王鸡          ⭐4.5  ¥15起  (最便宜)                              │  │
│ │ 杨铭宇          ⭐4.8  ¥18起                                       │  │
│ │ 辣妈辣妹        ⭐4.3  ¥22起                                       │  │
│ └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│ 用户切换 Tab → 饿了么:                                                   │
│ ┌────────────────────────────────────────────────────────────────────┐  │
│ │ 美团        [饿了么 ▼]                                              │  │
│ ├────────────────────────────────────────────────────────────────────┤  │
│ │ 天然鸡       ⭐4.2  ¥14起  (最便宜)                                 │  │
│ │ 杨铭宇       ⭐4.8  ¥19起                                          │  │
│ └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│ System Agent 附加比较摘要:                                                │
│ "综合来看，老王鸡(美团 ¥15)比天然鸡(饿了么 ¥14)价格接近,                   │
│  但杨铭宇在两平台均可点，饿了么略贵 ¥1"                                   │
└──────────────────────────────────────────────────────────────────────────┘
```

## 9. 文件结构

```
nano-llm/
├── src/main/java/com/nano/llm/
│   │
│   ├── service/                      # 系统服务
│   │   ├── INanoLLMService.kt        # 服务接口
│   │   └── NanoLLMService.kt         # 服务实现
│   │
│   ├── agent/                        # Agent 核心
│   │   ├── SystemAgent.kt            # 系统 Agent
│   │   ├── AppAgent.kt               # App Agent 接口
│   │   ├── BaseAppAgent.kt           # App Agent 基类
│   │   ├── AgentRegistry.kt          # Agent 注册表
│   │   └── AgentMessage.kt           # Agent 消息定义
│   │
│   ├── intent/                       # 意图系统
│   │   ├── IntentType.kt             # 意图类型
│   │   ├── IntentUnderstanding.kt    # 意图理解结果
│   │   └── EntityType.kt             # 实体类型
│   │
│   ├── a2ui/                         # A2UI 协议
│   │   ├── A2UISpec.kt               # A2UI 规格定义
│   │   ├── A2UIComponents.kt         # 组件定义
│   │   ├── A2UIAction.kt             # 动作定义
│   │   ├── A2UIGenerator.kt          # A2UI 生成器
│   │   └── A2UIRenderer.kt           # A2UI 渲染器
│   │
│   ├── conversation/                 # 对话管理
│   │   ├── Conversation.kt           # 对话
│   │   ├── ConversationManager.kt    # 对话管理器
│   │   └── ConversationContext.kt    # 上下文
│   │
│   ├── provider/                     # LLM 提供商
│   │   ├── LLMProvider.kt            # 提供商接口
│   │   ├── OpenAIProvider.kt         # OpenAI
│   │   ├── ClaudeProvider.kt         # Claude
│   │   └── MockProvider.kt           # 测试用
│   │
│   ├── model/                        # 数据模型
│   │   ├── LLMModels.kt              # LLM 相关模型
│   │   └── ResponseModels.kt         # 响应模型
│   │
│   └── prompt/                       # 提示词
│       └── PromptTemplates.kt        # 提示词模板
│
├── src/test/java/com/nano/llm/
│   ├── agent/
│   │   ├── SystemAgentTest.kt
│   │   └── AgentRegistryTest.kt
│   ├── a2ui/
│   │   ├── A2UIGeneratorTest.kt
│   │   └── A2UIRendererTest.kt
│   └── intent/
│       └── IntentUnderstandingTest.kt
│
└── DESIGN.md                         # 本设计文档
```

## 10. 实现优先级

### Phase 1: 核心框架
1. Agent 通信协议 (AgentMessage)
2. System Agent 基础实现
3. App Agent 接口和基类
4. Agent 注册表

### Phase 2: A2UI 协议
1. A2UI 组件定义
2. A2UI 渲染引擎（生成 NanoView）
3. A2UI 动作处理

### Phase 3: LLM 集成
1. LLM Provider 抽象
2. 意图理解实现
3. A2UI 生成器（LLM 生成模式）

### Phase 4: 示例 Agent
1. 系统设置 Agent
2. 示例 App Agent（模拟美团）

### Phase 5: 完善
1. 多轮对话
2. 错误处理
3. 权限控制
