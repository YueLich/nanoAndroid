package com.nano.sample.flight

import com.nano.llm.agent.*

class ChinaSouthernAgent : BaseAppAgent(
    agentId = "china_southern",
    agentName = "南方航空"
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
        val departure = request.entities["departure"] ?: return needMoreInfo("出发城市")
        val arrival = request.entities["arrival"] ?: return needMoreInfo("到达城市")
        val date = request.entities["date"] ?: return needMoreInfo("出发日期")

        // 模拟南航API（只返回南航自己的航班 + 联盟航班）
        val flights = mockChinaSouthernAPI(departure, arrival, date)

        if (flights.isEmpty()) {
            return TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "南方航空未找到符合条件的航班"
            )
        }

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
                    "source" to "china_southern"
                )
            },
            metadata = mapOf(
                "platform" to "南方航空",
                "count" to flights.size.toString()
            )
        )

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "南方航空找到 ${flights.size} 个航班",
            data = responseData
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
                metadata = mapOf("platform" to "南方航空")
            )
        )
    }

    private fun mockChinaSouthernAPI(departure: String, arrival: String, date: String): List<FlightInfo> {
        return listOf(
            FlightInfo(
                flightNumber = "CZ9012",
                airline = "中国南方航空",
                departure = Airport("PEK", "北京首都国际机场", "北京"),
                arrival = Airport("PVG", "上海浦东国际机场", "上海"),
                departureTime = parseDateTime("$date 09:00"),
                arrivalTime = parseDateTime("$date 11:20"),
                price = 650.0,
                cabinClass = "经济舱",
                remainingSeats = 20,
                sources = listOf("china_southern")
            ),
            // 联盟航班（与携程重复）
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
                sources = listOf("china_southern")
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
            supportedIntents = listOf("search_flight", "book_flight"),
            supportedEntities = listOf("departure", "arrival", "date", "flight_number"),
            exampleQueries = listOf(
                "南航查询航班",
                "南方航空北京到上海"
            ),
            responseTypes = setOf(ResponseType.RAW_DATA)
        )
    }
}
