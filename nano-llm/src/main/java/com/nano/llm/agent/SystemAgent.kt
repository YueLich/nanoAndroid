package com.nano.llm.agent

import com.nano.llm.a2ui.*
import com.nano.llm.intent.IntentUnderstanding

/** System Agent 响应 */
data class SystemAgentResponse(
    val message: String,
    val a2ui: A2UISpec? = null,
    val conversationState: TaskStatus,
    val followUpSuggestions: List<String> = emptyList(),
    val participatingAgents: List<String> = emptyList()
)

/**
 * System Agent - 系统智能体
 *
 * 核心能力:
 * 1. 多 Agent 选择与筛选
 * 2. 协调执行
 * 3. 响应聚合
 * 4. 构建最终响应
 */
class SystemAgent(
    private val agentRegistry: AgentRegistry,
    private val agentCoordinator: AgentCoordinator,
    private val responseAggregator: ResponseAggregator
) {

    /**
     * 处理意图理解结果 - 核心入口
     * @param understanding 由外部（LLM）解析出的意图理解结果
     * @param context 对话上下文
     */
    suspend fun processRequest(
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): SystemAgentResponse {
        // 1. 选择匹配的 Agent
        val selectedAgents = selectAgents(understanding)

        if (selectedAgents.isEmpty()) {
            return SystemAgentResponse(
                message = "抱歉，没有找到能处理此请求的应用",
                conversationState = TaskStatus.FAILED
            )
        }

        // 2. 协调执行
        val responses = agentCoordinator.coordinate(
            agents = selectedAgents,
            understanding = understanding,
            context = context
        )

        // 3. 响应聚合
        val aggregated = responseAggregator.aggregate(
            responses = responses,
            understanding = understanding
        )

        // 4. 构建最终响应
        return buildFinalResponse(aggregated)
    }

    /**
     * 选择匹配的 Agent
     *
     * 策略:
     * - 用户明确指定 app → 仅选对应 Agent
     * - 未指定 app 但有能力需求 → 根据能力广播
     * - 系统操作 → 仅系统设置 Agent
     * - 兜底 → 默认 Agent
     */
    fun selectAgents(understanding: IntentUnderstanding): List<AppAgent> {
        return when {
            // 用户明确指定了应用
            understanding.targetApps.isNotEmpty() -> {
                understanding.targetApps.mapNotNull { agentRegistry.getAgent(it) }
            }

            // 根据能力广播到多个 Agent
            understanding.broadcastCapability != null -> {
                agentRegistry.findAgentsByCapability(understanding.broadcastCapability)
            }

            // 系统级意图
            understanding.isSystemIntent() -> {
                listOfNotNull(agentRegistry.getSystemSettingsAgent())
            }

            // 兜底: 默认 Agent
            else -> listOfNotNull(agentRegistry.getDefaultAgent())
        }
    }

    /** 构建最终响应 */
    private fun buildFinalResponse(aggregated: AggregatedResponse): SystemAgentResponse {
        // 优先使用 A2UI 响应
        val a2ui = when {
            aggregated.a2uiResponses.size == 1 -> {
                aggregated.a2uiResponses.first().first
            }
            aggregated.a2uiResponses.size > 1 -> {
                // 多个 A2UI 需要合并 (后续由 A2UIGenerator 处理)
                aggregated.a2uiResponses.first().first
            }
            // 如果没有 A2UI 响应但有合并的数据，尝试生成 A2UI
            aggregated.mergedData != null -> {
                generateA2UIFromMergedData(aggregated.mergedData)
            }
            else -> null
        }

        return SystemAgentResponse(
            message = aggregated.summary,
            a2ui = a2ui,
            conversationState = aggregated.overallState,
            followUpSuggestions = aggregated.allFollowUpActions.map { it.label },
            participatingAgents = aggregated.participatingAgents
        )
    }

    /**
     * 从合并的数据生成 A2UI
     */
    private fun generateA2UIFromMergedData(mergedData: MergedResponseData): A2UISpec? {
        // 检查是否为航班数据（通过检查第一个项目的键来判断）
        val isFlightData = mergedData.items.firstOrNull()?.data?.containsKey("flight_number") == true

        return if (isFlightData) {
            generateFlightListA2UI(mergedData)
        } else {
            // 其他数据类型的通用 A2UI 生成
            generateGenericListA2UI(mergedData)
        }
    }

    /**
     * 为航班数据生成统一的 A2UI
     */
    private fun generateFlightListA2UI(mergedData: MergedResponseData): A2UISpec {
        val flightCards = mergedData.items.map { item ->
            createFlightCard(item)
        }

        val root = A2UIContainer(
            id = "flight_list_container",
            direction = Direction.VERTICAL,
            style = A2UIStyle(
                padding = Spacing(16, 16, 16, 16)
            ),
            children = listOf(
                // 标题栏
                A2UIText(
                    text = "✈️ 航班查询结果 (共${mergedData.uniqueCount}个)",
                    textStyle = TextStyle(fontSize = 20, fontWeight = FontWeight.BOLD)
                )
            ) + flightCards
        )

        return A2UISpec(version = "1.0", root = root)
    }

    /**
     * 创建航班卡片
     */
    private fun createFlightCard(item: MergedItem): A2UICard {
        val data = item.data
        val flightNumber = data["flight_number"] ?: ""
        val airline = data["airline"] ?: ""
        val departureAirport = data["departure_airport"] ?: ""
        val arrivalAirport = data["arrival_airport"] ?: ""
        val departureTime = data["departure_time"]?.substringAfter("T")?.substring(0, 5) ?: ""
        val arrivalTime = data["arrival_time"]?.substringAfter("T")?.substring(0, 5) ?: ""
        val price = data["price"] ?: ""
        val sources = item.sources.joinToString(", ")

        return A2UICard(
            id = "flight_card_$flightNumber",
            style = A2UIStyle(
                margin = Spacing(8, 0, 8, 0),
                backgroundColor = "#F5F5F5",
                borderRadius = 8
            ),
            content = A2UIContainer(
                direction = Direction.HORIZONTAL,
                children = listOf(
                    // 左侧：航班信息
                    A2UIContainer(
                        direction = Direction.VERTICAL,
                        style = A2UIStyle(width = 0, padding = Spacing(12, 12, 12, 12)),
                        children = listOf(
                            A2UIText(
                                text = "$flightNumber $airline",
                                textStyle = TextStyle(fontSize = 16, fontWeight = FontWeight.BOLD)
                            ),
                            A2UIText(
                                text = "$departureAirport → $arrivalAirport",
                                textStyle = TextStyle(fontSize = 14)
                            ),
                            A2UIText(
                                text = "$departureTime - $arrivalTime",
                                textStyle = TextStyle(fontSize = 14, color = "#666666")
                            )
                        )
                    ),
                    // 右侧：价格和按钮
                    A2UIContainer(
                        direction = Direction.VERTICAL,
                        style = A2UIStyle(padding = Spacing(12, 12, 12, 12)),
                        children = listOf(
                            A2UIText(
                                text = "¥$price",
                                textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD, color = "#FF5722")
                            ),
                            A2UIText(
                                text = "来源: $sources",
                                textStyle = TextStyle(fontSize = 12, color = "#999999")
                            ),
                            A2UIButton(
                                text = "预订",
                                action = A2UIAction(
                                    type = ActionType.AGENT_CALL,
                                    target = "ctrip", // 默认使用携程预订
                                    method = "book_flight",
                                    params = mapOf("flight_number" to flightNumber)
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    /**
     * 为通用数据生成列表 A2UI
     */
    private fun generateGenericListA2UI(mergedData: MergedResponseData): A2UISpec {
        val listItems = mergedData.items.map { item ->
            A2UIListItem(
                id = item.key,
                title = item.data["name"] ?: item.data["title"] ?: item.key,
                subtitle = item.data["description"] ?: "",
                trailing = item.sources.joinToString(", "),
                data = item.data
            )
        }

        val root = A2UIList(
            id = "generic_list",
            items = listItems
        )

        return A2UISpec(version = "1.0", root = root)
    }
}
