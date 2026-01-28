package com.nano.sample.notepad

import com.nano.llm.agent.*
import com.nano.llm.a2ui.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NotepadAgentTest {

    private fun createAgent(): NotepadAgent = NotepadAgent()

    // --- CRUD 基本操作 ---

    @Test
    fun testAddNote() {
        val agent = createAgent()
        val response = agent.addNote("测试标题", "测试内容")

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertNotNull(response.data!!.items[0]["note_id"])
        assertEquals(1, agent.getNoteCount())
    }

    @Test
    fun testAddMultipleNotes() {
        val agent = createAgent()
        agent.addNote("第一条", "内容A")
        agent.addNote("第二条", "内容B")
        agent.addNote("第三条", "内容C")

        assertEquals(3, agent.getNoteCount())
    }

    @Test
    fun testEditNote() {
        val agent = createAgent()
        val addResponse = agent.addNote("原始标题", "原始内容")
        val noteId = addResponse.data!!.items[0]["note_id"]!!

        val editResponse = agent.editNote(noteId, "更新标题", "更新内容")
        assertEquals(TaskStatus.SUCCESS, editResponse.status)

        val note = agent.getNote(noteId)!!
        assertEquals("更新标题", note.title)
        assertEquals("更新内容", note.content)
    }

    @Test
    fun testEditNotePartial() {
        val agent = createAgent()
        val addResponse = agent.addNote("原始标题", "原始内容")
        val noteId = addResponse.data!!.items[0]["note_id"]!!

        // 仅修改标题
        agent.editNote(noteId, "新标题", null)
        val note = agent.getNote(noteId)!!
        assertEquals("新标题", note.title)
        assertEquals("原始内容", note.content)
    }

    @Test
    fun testEditNonExistentNote() {
        val agent = createAgent()
        val response = agent.editNote("nonexistent", "title", "content")

        assertEquals(TaskStatus.FAILED, response.status)
    }

    @Test
    fun testDeleteNote() {
        val agent = createAgent()
        val addResponse = agent.addNote("待删除", "内容")
        val noteId = addResponse.data!!.items[0]["note_id"]!!

        val deleteResponse = agent.deleteNote(noteId)
        assertEquals(TaskStatus.SUCCESS, deleteResponse.status)
        assertEquals(0, agent.getNoteCount())
        assertNull(agent.getNote(noteId))
    }

    @Test
    fun testDeleteNonExistentNote() {
        val agent = createAgent()
        val response = agent.deleteNote("nonexistent")

        assertEquals(TaskStatus.FAILED, response.status)
    }

    @Test
    fun testListNotes() {
        val agent = createAgent()
        agent.addNote("Note A", "Content A")
        agent.addNote("Note B", "Content B")

        val response = agent.listNotes()
        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(2, response.data!!.items.size)
        assertNotNull(response.a2ui)
    }

    @Test
    fun testListEmptyNotes() {
        val agent = createAgent()
        val response = agent.listNotes()

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(0, response.data!!.items.size)
    }

    // --- 搜索 ---

    @Test
    fun testSearchNotes() {
        val agent = createAgent()
        agent.addNote("Kotlin 学习", "学习 Kotlin 语言")
        agent.addNote("Android 开发", "Android 应用开发")
        agent.addNote("算法", "数据结构与算法")

        val response = agent.searchNotes("Android")
        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(1, response.data!!.items.size)
    }

    @Test
    fun testSearchByContent() {
        val agent = createAgent()
        agent.addNote("笔记1", "包含关键词 target 的内容")
        agent.addNote("笔记2", "没有关键词")

        val response = agent.searchNotes("target")
        assertEquals(1, response.data!!.items.size)
    }

    @Test
    fun testSearchCaseInsensitive() {
        val agent = createAgent()
        agent.addNote("KOTLIN", "内容")

        val response = agent.searchNotes("kotlin")
        assertEquals(1, response.data!!.items.size)
    }

    @Test
    fun testSearchNoResults() {
        val agent = createAgent()
        agent.addNote("笔记", "内容")

        val response = agent.searchNotes("不存在的关键词")
        assertEquals(0, response.data!!.items.size)
    }

    // --- handleUIAction ---

    @Test
    fun testHandleUIActionListNotes() = runTest {
        val agent = createAgent()
        agent.addNote("Test", "Content")

        val action = A2UIAction(
            ActionType.AGENT_CALL, "notepad", "list",
            mapOf("intent" to "list_notes")
        )
        val response = agent.handleUIAction(action)
        assertEquals(TaskStatus.SUCCESS, response.status)
    }

    @Test
    fun testHandleUIActionAddNote() = runTest {
        val agent = createAgent()

        val action = A2UIAction(
            ActionType.AGENT_CALL, "notepad", "add",
            mapOf("intent" to "add_note", "title" to "UI新笔记", "content" to "UI内容")
        )
        val response = agent.handleUIAction(action)
        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(1, agent.getNoteCount())
    }

    @Test
    fun testHandleUIActionDeleteNote() = runTest {
        val agent = createAgent()
        val addResponse = agent.addNote("待删", "内容")
        val noteId = addResponse.data!!.items[0]["note_id"]!!

        val action = A2UIAction(
            ActionType.AGENT_CALL, "notepad", "delete",
            mapOf("intent" to "delete_note", "note_id" to noteId)
        )
        val response = agent.handleUIAction(action)
        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(0, agent.getNoteCount())
    }

    // --- handleRequest (通过 AgentMessage 调用) ---

    @Test
    fun testHandleRequestAddNote() = runTest {
        val agent = createAgent()
        val request = AgentMessage(
            messageId = "msg_1",
            from = AgentIdentity(AgentType.SYSTEM, "system", "System"),
            to = AgentIdentity(AgentType.APP, "notepad", "笔记本"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo("add_note", "add", 0.9f),
                entities = mapOf("intent" to "add_note", "title" to "消息笔记", "content" to "消息内容"),
                userQuery = "新增笔记",
                expectedResponseType = ResponseType.A2UI_JSON
            )
        )
        val response = agent.handleRequest(request)

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(1, agent.getNoteCount())
    }

    @Test
    fun testHandleRequestSearchNotes() = runTest {
        val agent = createAgent()
        agent.addNote("Kotlin 教程", "学习 Kotlin")
        agent.addNote("Java 基础", "学习 Java")

        val request = AgentMessage(
            messageId = "msg_2",
            from = AgentIdentity(AgentType.SYSTEM, "system", "System"),
            to = AgentIdentity(AgentType.APP, "notepad", "笔记本"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo("search_notes", "search", 0.9f),
                entities = mapOf("intent" to "search_notes", "keyword" to "Kotlin"),
                userQuery = "搜索 Kotlin",
                expectedResponseType = ResponseType.A2UI_JSON
            )
        )
        val response = agent.handleRequest(request)

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals(1, response.data!!.items.size)
    }

    // --- Agent 元数据 ---

    @Test
    fun testAgentIdentity() {
        val agent = createAgent()
        assertEquals("notepad", agent.agentId)
        assertEquals("笔记本", agent.agentName)
        assertTrue(agent.capabilities.contains(AgentCapability.SETTINGS))
    }

    @Test
    fun testDescribeCapabilities() {
        val agent = createAgent()
        val desc = agent.describeCapabilities()

        assertEquals("notepad", desc.agentId)
        assertTrue(desc.supportedIntents.contains("add_note"))
        assertTrue(desc.supportedIntents.contains("search_notes"))
        assertTrue(desc.responseTypes.contains(ResponseType.A2UI_JSON))
    }
}
