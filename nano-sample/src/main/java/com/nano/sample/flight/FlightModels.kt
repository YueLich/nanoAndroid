package com.nano.sample.flight

import java.time.LocalDateTime

/**
 * 航班信息
 */
data class FlightInfo(
    val flightNumber: String,      // 航班号 (e.g., "CA1234")
    val airline: String,            // 航空公司 (e.g., "中国国际航空")
    val departure: Airport,         // 出发机场
    val arrival: Airport,           // 到达机场
    val departureTime: LocalDateTime,  // 出发时间
    val arrivalTime: LocalDateTime,    // 到达时间
    val price: Double,              // 价格（元）
    val cabinClass: String = "经济舱",  // 舱位等级
    val remainingSeats: Int? = null,   // 剩余座位
    val sources: List<String> = emptyList()  // 数据来源（用于去重）
) {
    val duration: Long  // 飞行时长（分钟）
        get() = java.time.Duration.between(departureTime, arrivalTime).toMinutes()
}

/**
 * 机场信息
 */
data class Airport(
    val code: String,       // 三字码 (e.g., "PEK")
    val name: String,       // 机场名称 (e.g., "北京首都国际机场")
    val city: String        // 城市 (e.g., "北京")
)

/**
 * 航班查询请求
 */
data class FlightSearchRequest(
    val departureCity: String,
    val arrivalCity: String,
    val date: String,              // "YYYY-MM-DD"
    val cabinClass: String = "经济舱",
    val passengerCount: Int = 1
)

/**
 * 工具函数：解析日期时间字符串
 */
fun parseDateTime(dateTimeStr: String): LocalDateTime {
    val parts = dateTimeStr.split(" ")
    val date = parts[0]
    val time = parts[1]
    return LocalDateTime.parse("${date}T$time")
}
