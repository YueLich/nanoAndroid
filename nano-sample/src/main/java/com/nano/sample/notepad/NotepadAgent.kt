package com.nano.sample.notepad

import com.nano.llm.agent.*
import com.nano.llm.a2ui.*

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

class NotepadAgent : BaseAppAgent("notepad", "笔记本") {

    override val capabilities = setOf(AgentCapability.SETTINGS)

    private val notes = mutableMapOf<String, Note>()
    private var idCounter = 0

    override fun describeCapabilities() = AgentCapabilityDescription(
        agentId = agentId,
        supportedIntents = listOf("add_note", "edit_note", "delete_note", "list_notes", "search_notes"),
        supportedEntities = listOf("note_id", "title", "content", "keyword"),
        exampleQueries = listOf("新增笔记", "查看所有笔记", "删除笔记", "搜索笔记"),
        responseTypes = setOf(ResponseType.A2UI_JSON)
    )

    override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
        val intent = request.entities["intent"] ?: request.userQuery

        return when (intent) {
            "add_note" -> addNote(
                title = request.entities["title"] ?: "无标题",
                content = request.entities["content"] ?: ""
            )
            "edit_note" -> editNote(
                noteId = request.entities["note_id"] ?: return notFoundResponse(),
                title = request.entities["title"],
                content = request.entities["content"]
            )
            "delete_note" -> deleteNote(
                noteId = request.entities["note_id"] ?: return notFoundResponse()
            )
            "search_notes" -> searchNotes(
                keyword = request.entities["keyword"] ?: ""
            )
            else -> listNotes()
        }
    }

    override suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload {
        val intent = action.params?.get("intent") ?: "list_notes"
        val entities = action.params ?: emptyMap()

        return when (intent) {
            "add_note" -> addNote(
                title = entities["title"] ?: "无标题",
                content = entities["content"] ?: ""
            )
            "edit_note" -> editNote(
                noteId = entities["note_id"] ?: return notFoundResponse(),
                title = entities["title"],
                content = entities["content"]
            )
            "delete_note" -> deleteNote(
                noteId = entities["note_id"] ?: return notFoundResponse()
            )
            "search_notes" -> searchNotes(
                keyword = entities["keyword"] ?: ""
            )
            else -> listNotes()
        }
    }

    // --- CRUD 操作 ---

    fun addNote(title: String, content: String): TaskResponsePayload {
        idCounter++
        val note = Note(id = "note_$idCounter", title = title, content = content)
        notes[note.id] = note

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "笔记已创建: ${note.title}",
            data = ResponseData(
                type = "note",
                items = listOf(mapOf("note_id" to note.id, "title" to note.title))
            ),
            a2ui = buildNoteDetailSpec(note)
        )
    }

    fun editNote(noteId: String, title: String?, content: String?): TaskResponsePayload {
        val existing = notes[noteId] ?: return notFoundResponse()

        val updated = existing.copy(
            title = title ?: existing.title,
            content = content ?: existing.content,
            updatedAt = System.currentTimeMillis()
        )
        notes[noteId] = updated

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "笔记已更新: ${updated.title}",
            data = ResponseData(
                type = "note",
                items = listOf(mapOf("note_id" to noteId))
            ),
            a2ui = buildNoteDetailSpec(updated)
        )
    }

    fun deleteNote(noteId: String): TaskResponsePayload {
        val removed = notes.remove(noteId) ?: return notFoundResponse()

        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "笔记已删除: ${removed.title}",
            data = ResponseData(
                type = "note",
                items = listOf(mapOf("note_id" to noteId))
            )
        )
    }

    fun listNotes(): TaskResponsePayload {
        val spec = buildNoteListSpec(notes.values.toList())
        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "共 ${notes.size} 条笔记",
            data = ResponseData(
                type = "note_list",
                items = notes.values.map { mapOf("note_id" to it.id, "title" to it.title) }
            ),
            a2ui = spec
        )
    }

    fun searchNotes(keyword: String): TaskResponsePayload {
        val results = notes.values.filter { note ->
            note.title.contains(keyword, ignoreCase = true) ||
            note.content.contains(keyword, ignoreCase = true)
        }
        return TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "搜索结果: ${results.size} 条",
            data = ResponseData(
                type = "note_search",
                items = results.map { mapOf("note_id" to it.id, "title" to it.title) },
                metadata = mapOf("keyword" to keyword, "count" to results.size.toString())
            ),
            a2ui = buildNoteListSpec(results)
        )
    }

    fun getNote(noteId: String): Note? = notes[noteId]

    fun getNoteCount(): Int = notes.size

    private fun notFoundResponse() = TaskResponsePayload(
        status = TaskStatus.FAILED,
        message = "笔记未找到"
    )

    // --- UI Spec 构建 ---

    private fun buildNoteListSpec(noteList: List<Note>): A2UISpec {
        val items = noteList.map { note ->
            A2UIListItem(
                id = note.id,
                title = note.title,
                subtitle = note.content.take(30) + if (note.content.length > 30) "..." else "",
                data = mapOf("note_id" to note.id)
            )
        }

        return A2UISpec(
            root = A2UIContainer(
                id = "notepad_list",
                direction = Direction.VERTICAL,
                children = listOf(
                    A2UIText(
                        text = "笔记列表 (${noteList.size})",
                        textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD)
                    ),
                    A2UIList(id = "notes", items = items),
                    A2UIButton(
                        text = "新增笔记",
                        action = A2UIAction(ActionType.AGENT_CALL, "notepad", "add_note")
                    )
                )
            )
        )
    }

    private fun buildNoteDetailSpec(note: Note): A2UISpec {
        return A2UISpec(
            root = A2UICard(
                id = "note_detail_${note.id}",
                header = A2UIText(
                    text = note.title,
                    textStyle = TextStyle(fontSize = 20, fontWeight = FontWeight.BOLD)
                ),
                content = A2UIText(
                    text = note.content,
                    textStyle = TextStyle(fontSize = 14)
                ),
                footer = A2UIContainer(
                    direction = Direction.HORIZONTAL,
                    children = listOf(
                        A2UIButton(
                            text = "编辑",
                            action = A2UIAction(ActionType.AGENT_CALL, "notepad", "edit_note",
                                mapOf("note_id" to note.id, "intent" to "edit_note"))
                        ),
                        A2UIButton(
                            text = "删除",
                            action = A2UIAction(ActionType.AGENT_CALL, "notepad", "delete_note",
                                mapOf("note_id" to note.id, "intent" to "delete_note"))
                        )
                    )
                )
            )
        )
    }
}
