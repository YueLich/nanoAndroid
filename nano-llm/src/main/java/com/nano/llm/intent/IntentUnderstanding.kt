package com.nano.llm.intent

import com.nano.llm.agent.AgentCapability

/**
 * 意图理解结果
 */
data class IntentUnderstanding(
    val intentType: IntentType,
    val targetApps: List<String> = emptyList(),
    val broadcastCapability: AgentCapability? = null,
    val action: String,
    val entities: Map<String, String> = emptyMap(),
    val confidence: Float,
    val coordinationStrategy: CoordinationStrategy,
    val preferredLayout: MergeLayout,
    val needsClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val timeout: Long? = null,
    val sortPreference: SortPreference = SortPreference.RELEVANCE
) {
    /** 判断是否为系统级意图 */
    fun isSystemIntent(): Boolean = intentType == IntentType.SYSTEM_SETTINGS
}

/** 意图类型 */
enum class IntentType {
    APP_SEARCH,
    APP_ORDER,
    APP_NAVIGATE,
    SYSTEM_SETTINGS,
    GENERAL_CHAT
}

/** 协调策略 */
enum class CoordinationStrategy {
    PARALLEL,
    SEQUENTIAL,
    RACE,
    FALLBACK
}

/** 合并布局 */
enum class MergeLayout {
    TABS,
    UNIFIED_LIST,
    CARDS
}

/** 排序偏好 */
enum class SortPreference {
    RELEVANCE,
    PRICE_ASC,
    PRICE_DESC,
    RATING_DESC,
    DISTANCE_ASC
}
