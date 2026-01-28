package com.nano.llm.agent

import com.nano.llm.a2ui.A2UISpec
import com.nano.llm.intent.IntentUnderstanding

/** 合并后的单个项目 */
data class MergedItem(
    val data: Map<String, String>,
    val source: String? = null,
    val sources: List<String> = emptyList(),
    val key: String
)

/** 合并后的响应数据 */
data class MergedResponseData(
    val items: List<MergedItem>,
    val totalCount: Int,
    val uniqueCount: Int,
    val sources: List<String>
)

/** 聚合后的响应 */
data class AggregatedResponse(
    val a2uiResponses: List<Pair<A2UISpec, String>>,        // A2UI + agentId
    val rawDataResponses: List<Pair<ResponseData, String>>, // 原始数据 + agentId
    val mergedData: MergedResponseData?,
    val summary: String,
    val allFollowUpActions: List<FollowUpAction>,
    val participatingAgents: List<String>,
    val overallState: TaskStatus,
    val failedAgents: List<String>
)

/**
 * 响应聚合器 - 合并多个 Agent 的响应
 *
 * 核心能力:
 * 1. 去重: 同一条目在多平台出现时合并
 * 2. 排序: 按相关度排序
 * 3. 分类: 将响应按 A2UI / 原始数据分类
 * 4. 摘要: 生成汇总文本
 */
class ResponseAggregator {

    /** 聚合多个 Agent 的响应 */
    fun aggregate(
        responses: List<AgentResponse>,
        understanding: IntentUnderstanding
    ): AggregatedResponse {
        val successResponses = responses.filter { it.success && it.payload != null }

        // 分类响应
        val a2uiResponses = successResponses
            .filter { it.payload!!.a2ui != null }
            .map { it.payload!!.a2ui!! to it.agent.agentId }

        val rawDataResponses = successResponses
            .filter { it.payload!!.data != null && it.payload!!.a2ui == null }
            .map { it.payload!!.data!! to it.agent.agentId }

        // 合并原始数据（去重）
        val mergedData = if (rawDataResponses.isNotEmpty()) {
            mergeData(rawDataResponses)
        } else null

        // 生成摘要
        val summary = generateSummary(successResponses)

        // 合并后续操作
        val allFollowUpActions = successResponses
            .flatMap { it.payload!!.followUpActions ?: emptyList() }

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

    /** 合并多源原始数据，去重 */
    fun mergeData(
        rawResponses: List<Pair<ResponseData, String>>
    ): MergedResponseData {
        val allItems = rawResponses.flatMap { (data, agentId) ->
            data.items.map { item ->
                MergedItem(
                    data = item,
                    source = agentId,
                    key = extractDedupeKey(item)
                )
            }
        }

        // 按去重键分组
        val deduped = allItems
            .groupBy { it.key }
            .map { (key, items) ->
                MergedItem(
                    data = items.first().data,
                    sources = items.map { it.source ?: "" }.distinct(),
                    key = key
                )
            }

        return MergedResponseData(
            items = deduped,
            totalCount = allItems.size,
            uniqueCount = deduped.size,
            sources = rawResponses.map { it.second }.distinct()
        )
    }

    /** 提取去重键 - 优先使用 name, 其次 id */
    private fun extractDedupeKey(item: Map<String, String>): String {
        return item["name"] ?: item["id"] ?: item.entries.joinToString(",")
    }

    /** 生成摘要文本 */
    private fun generateSummary(responses: List<AgentResponse>): String {
        val sources = responses.map { it.agent.agentName }
        val totalItems = responses.sumOf { it.payload?.data?.items?.size ?: 0 }

        return when {
            sources.size > 1 ->
                "已从 ${sources.joinToString("、")} 共找到 $totalItems 个结果"
            sources.size == 1 ->
                responses.first().payload?.message ?: "已找到 $totalItems 个结果"
            else -> "抱歉，没有找到相关结果"
        }
    }

    /** 判断整体状态 */
    private fun determineOverallState(responses: List<AgentResponse>): TaskStatus {
        if (responses.isEmpty()) return TaskStatus.FAILED
        val statuses = responses.map { it.payload?.status }
        if (statuses.all { it == TaskStatus.SUCCESS }) return TaskStatus.SUCCESS
        if (statuses.any { it == TaskStatus.SUCCESS }) return TaskStatus.PARTIAL
        return TaskStatus.FAILED
    }
}
