package com.nano.llm.intent

import com.nano.llm.agent.AgentCapability
import org.junit.Test
import org.junit.Assert.*

class IntentUnderstandingTest {

    private fun createTestUnderstanding(
        intentType: IntentType = IntentType.APP_SEARCH,
        targetApps: List<String> = emptyList(),
        broadcastCapability: AgentCapability? = null,
        action: String = "search",
        entities: Map<String, String> = emptyMap(),
        confidence: Float = 0.9f,
        strategy: CoordinationStrategy = CoordinationStrategy.PARALLEL,
        layout: MergeLayout = MergeLayout.UNIFIED_LIST
    ): IntentUnderstanding {
        return IntentUnderstanding(
            intentType = intentType,
            targetApps = targetApps,
            broadcastCapability = broadcastCapability,
            action = action,
            entities = entities,
            confidence = confidence,
            coordinationStrategy = strategy,
            preferredLayout = layout
        )
    }

    @Test
    fun testBasicConstruction() {
        val intent = createTestUnderstanding(entities = mapOf("food" to "黄焖鸡"))
        assertEquals(IntentType.APP_SEARCH, intent.intentType)
        assertEquals("search", intent.action)
        assertEquals(0.9f, intent.confidence)
        assertEquals("黄焖鸡", intent.entities["food"])
    }

    @Test
    fun testIsSystemIntent() {
        val sysIntent = createTestUnderstanding(intentType = IntentType.SYSTEM_SETTINGS)
        assertTrue(sysIntent.isSystemIntent())

        val appIntent = createTestUnderstanding(intentType = IntentType.APP_SEARCH)
        assertFalse(appIntent.isSystemIntent())
    }

    @Test
    fun testNonSystemIntentTypes() {
        assertFalse(createTestUnderstanding(intentType = IntentType.APP_ORDER).isSystemIntent())
        assertFalse(createTestUnderstanding(intentType = IntentType.APP_NAVIGATE).isSystemIntent())
        assertFalse(createTestUnderstanding(intentType = IntentType.GENERAL_CHAT).isSystemIntent())
    }

    @Test
    fun testTargetAppsList() {
        val intent = createTestUnderstanding(targetApps = listOf("meituan", "eleme"))
        assertEquals(2, intent.targetApps.size)
        assertTrue(intent.targetApps.contains("meituan"))
        assertTrue(intent.targetApps.contains("eleme"))
    }

    @Test
    fun testEmptyTargetApps() {
        val intent = createTestUnderstanding(targetApps = emptyList())
        assertTrue(intent.targetApps.isEmpty())
    }

    @Test
    fun testBroadcastCapability() {
        val intent = createTestUnderstanding(broadcastCapability = AgentCapability.SEARCH)
        assertEquals(AgentCapability.SEARCH, intent.broadcastCapability)
    }

    @Test
    fun testBroadcastCapabilityNull() {
        val intent = createTestUnderstanding()
        assertNull(intent.broadcastCapability)
    }

    @Test
    fun testCoordinationStrategyParallel() {
        val intent = createTestUnderstanding(strategy = CoordinationStrategy.PARALLEL)
        assertEquals(CoordinationStrategy.PARALLEL, intent.coordinationStrategy)
    }

    @Test
    fun testCoordinationStrategySequential() {
        val intent = createTestUnderstanding(strategy = CoordinationStrategy.SEQUENTIAL)
        assertEquals(CoordinationStrategy.SEQUENTIAL, intent.coordinationStrategy)
    }

    @Test
    fun testCoordinationStrategyRace() {
        val intent = createTestUnderstanding(strategy = CoordinationStrategy.RACE)
        assertEquals(CoordinationStrategy.RACE, intent.coordinationStrategy)
    }

    @Test
    fun testCoordinationStrategyFallback() {
        val intent = createTestUnderstanding(strategy = CoordinationStrategy.FALLBACK)
        assertEquals(CoordinationStrategy.FALLBACK, intent.coordinationStrategy)
    }

    @Test
    fun testMergeLayoutTabs() {
        val intent = createTestUnderstanding(layout = MergeLayout.TABS)
        assertEquals(MergeLayout.TABS, intent.preferredLayout)
    }

    @Test
    fun testMergeLayoutUnifiedList() {
        val intent = createTestUnderstanding(layout = MergeLayout.UNIFIED_LIST)
        assertEquals(MergeLayout.UNIFIED_LIST, intent.preferredLayout)
    }

    @Test
    fun testMergeLayoutCards() {
        val intent = createTestUnderstanding(layout = MergeLayout.CARDS)
        assertEquals(MergeLayout.CARDS, intent.preferredLayout)
    }

    @Test
    fun testNeedsClarification() {
        val clear = createTestUnderstanding()
        assertFalse(clear.needsClarification)
        assertNull(clear.clarificationQuestion)

        val unclear = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            action = "search",
            confidence = 0.3f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST,
            needsClarification = true,
            clarificationQuestion = "你想搜索什么？"
        )
        assertTrue(unclear.needsClarification)
        assertEquals("你想搜索什么？", unclear.clarificationQuestion)
    }

    @Test
    fun testDefaultSortPreference() {
        val intent = createTestUnderstanding()
        assertEquals(SortPreference.RELEVANCE, intent.sortPreference)
    }

    @Test
    fun testCustomSortPreference() {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST,
            sortPreference = SortPreference.PRICE_ASC
        )
        assertEquals(SortPreference.PRICE_ASC, intent.sortPreference)
    }

    @Test
    fun testTimeoutNullByDefault() {
        val intent = createTestUnderstanding()
        assertNull(intent.timeout)
    }

    @Test
    fun testCustomTimeout() {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST,
            timeout = 5000L
        )
        assertEquals(5000L, intent.timeout)
    }

    @Test
    fun testEquality() {
        val intent1 = createTestUnderstanding(entities = mapOf("food" to "pizza"))
        val intent2 = createTestUnderstanding(entities = mapOf("food" to "pizza"))
        assertEquals(intent1, intent2)
    }

    @Test
    fun testInequalityDifferentEntities() {
        val intent1 = createTestUnderstanding(entities = mapOf("food" to "pizza"))
        val intent2 = createTestUnderstanding(entities = mapOf("food" to "burger"))
        assertNotEquals(intent1, intent2)
    }

    @Test
    fun testAllIntentTypes() {
        assertEquals(5, IntentType.values().size)
        assertNotNull(IntentType.valueOf("APP_SEARCH"))
        assertNotNull(IntentType.valueOf("APP_ORDER"))
        assertNotNull(IntentType.valueOf("SYSTEM_SETTINGS"))
        assertNotNull(IntentType.valueOf("GENERAL_CHAT"))
    }

    @Test
    fun testAllSortPreferences() {
        assertEquals(5, SortPreference.values().size)
        assertNotNull(SortPreference.valueOf("PRICE_ASC"))
        assertNotNull(SortPreference.valueOf("PRICE_DESC"))
        assertNotNull(SortPreference.valueOf("RATING_DESC"))
        assertNotNull(SortPreference.valueOf("DISTANCE_ASC"))
    }
}
