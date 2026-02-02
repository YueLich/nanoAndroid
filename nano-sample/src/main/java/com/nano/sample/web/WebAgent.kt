package com.nano.sample.web

import com.nano.llm.agent.*
import com.nano.sample.flight.Airport
import com.nano.sample.flight.FlightInfo
import com.nano.sample.flight.parseDateTime

/**
 * WebAgent - 浏览器工具Agent
 *
 * 模拟浏览器访问网页的能力，类似于OpenAI的Browser工具
 * 可以打开网页、填写表单、解析内容、提取信息
 *
 * 支持的操作：
 * - navigate(url) - 打开网页
 * - fill_form(fields) - 填写表单
 * - click(selector) - 点击元素
 * - extract(pattern) - 提取数据
 */
class WebAgent : BaseAppAgent(
    agentId = "web_agent",
    agentName = "网页浏览器"
) {
    override val capabilities = setOf(AgentCapability.SEARCH, AgentCapability.NAVIGATION)

    override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
        return when (request.intent.action) {
            "search_flight" -> searchFlightViaWeb(request)
            "navigate_web" -> navigateWeb(request)
            "extract_data" -> extractData(request)
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "未知操作: ${request.intent.action}"
            )
        }
    }

    /**
     * 通过访问去哪儿网查询航班
     */
    private fun searchFlightViaWeb(request: TaskRequestPayload): TaskResponsePayload {
        val departure = request.entities["departure"] ?: return needMoreInfo("出发城市")
        val arrival = request.entities["arrival"] ?: return needMoreInfo("到达城市")
        val date = request.entities["date"] ?: return needMoreInfo("出发日期")

        // 模拟浏览器操作流程
        val browserSteps = mutableListOf<String>()

        // 1. 打开去哪儿网
        browserSteps.add("打开 https://flight.qunar.com")

        // 2. 填写查询表单
        browserSteps.add("填写出发地：$departure")
        browserSteps.add("填写目的地：$arrival")
        browserSteps.add("选择日期：$date")

        // 3. 点击搜索按钮
        browserSteps.add("点击搜索按钮")

        // 4. 等待页面加载
        browserSteps.add("等待航班列表加载...")

        // 5. 解析航班数据（模拟）
        val flights = mockBrowserExtraction(departure, arrival, date)

        if (flights.isEmpty()) {
            return TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "通过浏览器访问去哪儿网未找到航班",
                data = ResponseData(
                    type = "web_result",
                    items = emptyList(),
                    metadata = mapOf(
                        "url" to "https://flight.qunar.com",
                        "steps" to browserSteps.joinToString(" → ")
                    )
                )
            )
        }

        // 6. 构建响应
        val responseData = ResponseData(
            type = "flights",
            items = flights.map { flight ->
                mapOf(
                    "flight_number" to flight.flightNumber,
                    "airline" to flight.airline,
                    "departure_airport" to flight.departure.code,
                    "arrival_airport" to flight.arrival.code,
                    "departure_time" to flight.departureTime.toString(),
                    "arrival_time" to flight.arrivalTime.toString(),
                    "price" to flight.price.toString(),
                    "cabin_class" to flight.cabinClass,
                    "source" to "web_agent"
                )
            },
            metadata = mapOf(
                "platform" to "去哪儿网",
                "count" to flights.size.toString(),
                "data_source" to "browser",
                "url" to "https://flight.qunar.com",
                "steps" to browserSteps.joinToString(" → ")
            )
        )

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "通过浏览器从去哪儿网找到 ${flights.size} 个航班",
            data = responseData,
            followUpActions = listOf(
                FollowUpAction(
                    id = "open_browser",
                    label = "打开网页查看详情",
                    actionType = "open_browser"
                )
            )
        )
    }

    /**
     * 模拟浏览器提取数据
     * 实际应该：
     * 1. 使用 WebView 或 Chromium 打开网页
     * 2. 使用 JavaScript 注入或 DOM 解析提取数据
     * 3. 处理动态加载内容（AJAX）
     */
    private fun mockBrowserExtraction(departure: String, arrival: String, date: String): List<FlightInfo> {
        // 模拟从去哪儿网页面提取的数据
        return listOf(
            // 与携程重复的航班（去哪儿网价格可能不同）
            FlightInfo(
                flightNumber = "MU5678",
                airline = "中国东方航空",
                departure = Airport("PEK", "北京首都国际机场", "北京"),
                arrival = Airport("SHA", "上海虹桥国际机场", "上海"),
                departureTime = parseDateTime("$date 10:00"),
                arrivalTime = parseDateTime("$date 12:20"),
                price = 620.0,  // 去哪儿网价格
                cabinClass = "经济舱",
                sources = listOf("web_agent")
            ),
            // 去哪儿网独有的航班
            FlightInfo(
                flightNumber = "HU3456",
                airline = "海南航空",
                departure = Airport("PEK", "北京首都国际机场", "北京"),
                arrival = Airport("PVG", "上海浦东国际机场", "上海"),
                departureTime = parseDateTime("$date 14:00"),
                arrivalTime = parseDateTime("$date 16:30"),
                price = 700.0,
                cabinClass = "经济舱",
                sources = listOf("web_agent")
            )
        )
    }

    /**
     * 通用网页导航功能
     */
    private fun navigateWeb(request: TaskRequestPayload): TaskResponsePayload {
        val url = request.entities["url"] ?: return needMoreInfo("目标网址")

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "已打开网页：$url",
            data = ResponseData(
                type = "web_page",
                items = listOf(mapOf("url" to url, "title" to "模拟页面")),
                metadata = mapOf("action" to "navigate")
            )
        )
    }

    /**
     * 数据提取功能
     */
    private fun extractData(request: TaskRequestPayload): TaskResponsePayload {
        val pattern = request.entities["pattern"] ?: return needMoreInfo("提取模式")

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "数据提取完成",
            data = ResponseData(
                type = "extracted_data",
                items = listOf(mapOf("pattern" to pattern, "result" to "模拟数据")),
                metadata = mapOf("action" to "extract")
            )
        )
    }

    private fun needMoreInfo(field: String): TaskResponsePayload {
        return TaskResponsePayload(
            status = TaskStatus.NEED_MORE_INFO,
            message = "请提供$field"
        )
    }

    override fun describeCapabilities(): AgentCapabilityDescription {
        return AgentCapabilityDescription(
            agentId = agentId,
            supportedIntents = listOf("search_flight", "navigate_web", "extract_data"),
            supportedEntities = listOf("departure", "arrival", "date", "url", "pattern"),
            exampleQueries = listOf(
                "通过网页查询航班",
                "打开去哪儿网",
                "提取页面数据"
            ),
            responseTypes = setOf(ResponseType.RAW_DATA)
        )
    }
}
