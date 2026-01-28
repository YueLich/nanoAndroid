package com.nano.sample.calculator

import com.nano.llm.agent.*
import com.nano.llm.a2ui.*

class CalculatorAgent : BaseAppAgent("calculator", "计算器") {

    override val capabilities = setOf(AgentCapability.SEARCH)

    override fun describeCapabilities() = AgentCapabilityDescription(
        agentId = agentId,
        supportedIntents = listOf("calculate", "compute", "math", "eval"),
        supportedEntities = listOf("expression", "number", "operator"),
        exampleQueries = listOf("1 + 2", "calculate 3 * 4 + 5", "what is 100 / 4"),
        responseTypes = setOf(ResponseType.A2UI_JSON, ResponseType.RAW_DATA)
    )

    override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
        val expression = request.entities["expression"] ?: request.userQuery

        return try {
            val result = evaluate(expression)
            val spec = buildResultSpec(expression, result)

            TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "$expression = ${formatResult(result)}",
                data = ResponseData(
                    type = "calculation",
                    items = listOf(mapOf("expression" to expression, "result" to formatResult(result)))
                ),
                a2ui = spec
            )
        } catch (e: CalculationException) {
            TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = e.message ?: "计算错误"
            )
        }
    }

    override suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload {
        val expression = action.params?.get("expression") ?: return TaskResponsePayload(
            status = TaskStatus.FAILED, message = "缺少表达式参数"
        )
        return try {
            val result = evaluate(expression)
            val spec = buildResultSpec(expression, result)

            TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "$expression = ${formatResult(result)}",
                data = ResponseData(
                    type = "calculation",
                    items = listOf(mapOf("expression" to expression, "result" to formatResult(result)))
                ),
                a2ui = spec
            )
        } catch (e: CalculationException) {
            TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = e.message ?: "计算错误"
            )
        }
    }

    // --- 表达式求值 ---

    fun evaluate(expression: String): Double {
        val tokens = tokenize(expression.trim())
        if (tokens.isEmpty()) throw CalculationException("空表达式")
        val parser = ExpressionParser(tokens)
        val result = parser.parseExpression()
        if (parser.pos < tokens.size) {
            throw CalculationException("未预期的符号: ${tokens[parser.pos]}")
        }
        return result
    }

    fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.6g", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun tokenize(expr: String): List<String> {
        val sanitizedExpr = expr
            .replace('＋', '+')
            .replace('－', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace('（', '(')
            .replace('）', ')')

        val tokens = mutableListOf<String>()
        var i = 0
        while (i < sanitizedExpr.length) {
            when {
                sanitizedExpr[i].isWhitespace() -> i++
                sanitizedExpr[i].isDigit() || sanitizedExpr[i] == '.' -> {
                    val start = i
                    while (i < sanitizedExpr.length && (sanitizedExpr[i].isDigit() || sanitizedExpr[i] == '.')) i++
                    tokens.add(sanitizedExpr.substring(start, i))
                }
                sanitizedExpr[i] in "+-*/()^%" -> {
                    tokens.add(sanitizedExpr[i].toString())
                    i++
                }
                // Skip other characters
                else -> i++
            }
        }
        return tokens
    }

    // 递归下降解析器：支持 +, -, *, /, ^, %, ()
    inner class ExpressionParser(private val tokens: List<String>) {
        var pos = 0

        fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < tokens.size && tokens[pos] in listOf("+", "-")) {
                val op = tokens[pos++]
                val right = parseTerm()
                left = if (op == "+") left + right else left - right
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parsePower()
            while (pos < tokens.size && tokens[pos] in listOf("*", "/", "%")) {
                val op = tokens[pos++]
                val right = parsePower()
                left = when (op) {
                    "*" -> left * right
                    "/" -> {
                        if (right == 0.0) throw CalculationException("除以零")
                        left / right
                    }
                    "%" -> {
                        if (right == 0.0) throw CalculationException("模零")
                        left % right
                    }
                    else -> left
                }
            }
            return left
        }

        private fun parsePower(): Double {
            val base = parseFactor()
            if (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val exp = parsePower() // 右结合
                return Math.pow(base, exp)
            }
            return base
        }

        private fun parseFactor(): Double {
            if (pos >= tokens.size) throw CalculationException("表达式不完整")

            // 一元正负号
            if (tokens[pos] == "-") {
                pos++
                return -parseFactor()
            }
            if (tokens[pos] == "+") {
                pos++
                return parseFactor()
            }

            // 括号
            if (tokens[pos] == "(") {
                pos++ // skip '('
                val result = parseExpression()
                if (pos >= tokens.size || tokens[pos] != ")") {
                    throw CalculationException("缺少右括号")
                }
                pos++ // skip ')'
                return result
            }

            // 数字
            val token = tokens[pos]
            pos++
            return token.toDoubleOrNull() ?: throw CalculationException("无效数字: $token")
        }
    }

    private fun buildResultSpec(expression: String, result: Double): A2UISpec {
        val displayResult = formatResult(result)
        return A2UISpec(
            root = A2UICard(
                id = "calc_result",
                header = A2UIText(text = "计算结果", textStyle = TextStyle(fontWeight = FontWeight.BOLD)),
                content = A2UIContainer(
                    direction = Direction.VERTICAL,
                    children = listOf(
                        A2UIText(text = "表达式: $expression", textStyle = TextStyle(fontSize = 14)),
                        A2UIText(text = "结果: $displayResult", textStyle = TextStyle(fontSize = 24, fontWeight = FontWeight.BOLD))
                    )
                ),
                footer = A2UIButton(
                    text = "清除",
                    action = A2UIAction(ActionType.AGENT_CALL, "calculator", "clear")
                )
            )
        )
    }
}

class CalculationException(message: String) : Exception(message)
