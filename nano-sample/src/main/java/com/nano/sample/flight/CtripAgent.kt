package com.nano.sample.flight

import com.nano.llm.agent.*

class CtripAgent : BaseAppAgent(
    agentId = "ctrip",
    agentName = "携程旅行"
) {
    override val capabilities = setOf(AgentCapability.SEARCH, AgentCapability.ORDER)

    override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
        return when (request.intent.action) {
            "search_flight" -> searchFlights(request)
            "book_flight" -> bookFlight(request)
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "未知操作: ${request.intent.action}"
            )
        }
    }

    private fun searchFlights(request: TaskRequestPayload): TaskResponsePayload {
        // 1. 提取查询参数
        val departure = request.entities["departure"] ?: return needMoreInfo("出发城市")
        val arrival = request.entities["arrival"] ?: return needMoreInfo("到达城市")
        val date = request.entities["date"] ?: return needMoreInfo("出发日期")

        // 2. 模拟调用携程API
        val flights = mockCtripAPI(departure, arrival, date)

        if (flights.isEmpty()) {
            return TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "携程未找到符合条件的航班"
            )
        }

        // 3. 生成 ResponseData
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
                    "source" to "ctrip"
                )
            },
            metadata = mapOf(
                "platform" to "携程",
                "count" to flights.size.toString()
            )
        )

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "携程找到 ${flights.size} 个航班",
            data = responseData,
            followUpActions = listOf(
                FollowUpAction(
                    id = "view_price_trend",
                    label = "查看价格走势",
                    actionType = "view_price_trend"
                ),
                FollowUpAction(
                    id = "set_price_alert",
                    label = "设置价格提醒",
                    actionType = "set_price_alert"
                )
            )
        )
    }

    private fun bookFlight(request: TaskRequestPayload): TaskResponsePayload {
        val flightNumber = request.entities["flight_number"] ?: return needMoreInfo("航班号")

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "航班 $flightNumber 预订功能开发中",
            data = ResponseData(
                type = "booking_result",
                items = listOf(mapOf("flight_number" to flightNumber, "status" to "pending")),
                metadata = mapOf("platform" to "携程")
            )
        )
    }

    /**
     * 模拟携程API（实际应该调用真实API）
     */
    private fun mockCtripAPI(departure: String, arrival: String, date: String): List<FlightInfo> {
        return listOf(
            FlightInfo(
                flightNumber = "CA1234",
                airline = "中国国际航空",
                departure = Airport("PEK", "北京首都国际机场", "北京"),
                arrival = Airport("PVG", "上海浦东国际机场", "上海"),
                departureTime = parseDateTime("$date 08:00"),
                arrivalTime = parseDateTime("$date 10:30"),
                price = 580.0,
                cabinClass = "经济舱",
                remainingSeats = 15,
                sources = listOf("ctrip")
            ),
            FlightInfo(
                flightNumber = "MU5678",
                airline = "中国东方航空",
                departure = Airport("PEK", "北京首都国际机场", "北京"),
                arrival = Airport("SHA", "上海虹桥国际机场", "上海"),
                departureTime = parseDateTime("$date 10:00"),
                arrivalTime = parseDateTime("$date 12:20"),
                price = 620.0,
                cabinClass = "经济舱",
                remainingSeats = 8,
                sources = listOf("ctrip")
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
            supportedIntents = listOf("search_flight", "book_flight", "view_order"),
            supportedEntities = listOf("departure", "arrival", "date", "cabin_class", "flight_number"),
            exampleQueries = listOf(
                "查询北京到上海的机票",
                "预订明天去广州的航班",
                "查看我的订单"
            ),
            responseTypes = setOf(ResponseType.RAW_DATA)
        )
    }
}
