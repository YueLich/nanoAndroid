# LLM 配置指南

NanoAndroid 支持多种大语言模型 (LLM) 提供商，用于实现自然语言交互和意图理解。

## 快速开始

### 1. 复制配置模板

```bash
cp local.properties.example local.properties
```

### 2. 编辑 `local.properties`

```properties
# 设置 Android SDK 路径
sdk.dir=/path/to/android/sdk

# LLM 配置
llm.provider=openrouter
llm.apiKey=YOUR_API_KEY_HERE
llm.model=google/gemini-2.0-flash-exp:free
llm.baseUrl=https://openrouter.ai/api/v1
```

### 3. 重新构建项目

```bash
./gradlew clean :app:installDebug
```

---

## 支持的 Provider

### 1. OpenRouter (推荐)

**优势**：
- ✅ 支持多种模型（OpenAI、Claude、Gemini、Llama 等）
- ✅ 有免费模型可用
- ✅ 统一接口，方便切换模型
- ✅ 按量付费，价格透明

**注册**: [https://openrouter.ai](https://openrouter.ai)
**获取 API Key**: [https://openrouter.ai/keys](https://openrouter.ai/keys)

**配置示例**：

```properties
llm.provider=openrouter
llm.apiKey=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=google/gemini-2.0-flash-exp:free  # 免费模型
llm.baseUrl=https://openrouter.ai/api/v1
```

**推荐模型**：
- `google/gemini-2.0-flash-exp:free` - Gemini 2.0 Flash (免费，速度快)
- `meta-llama/llama-3.1-8b-instruct:free` - Llama 3.1 8B (免费)
- `microsoft/phi-3-mini-128k-instruct:free` - Phi-3 Mini (免费)
- `anthropic/claude-3.5-sonnet` - Claude 3.5 Sonnet (付费，效果好)
- `openai/gpt-4o` - GPT-4o (付费，效果好)

**查看所有模型**: [https://openrouter.ai/models](https://openrouter.ai/models)

---

### 2. Groq

**优势**：
- ✅ 推理速度极快 (比 OpenAI 快 10-20 倍)
- ✅ 免费额度大
- ✅ 支持 Llama、Mixtral 等开源模型

**注册**: [https://console.groq.com](https://console.groq.com)
**获取 API Key**: [https://console.groq.com/keys](https://console.groq.com/keys)

**配置示例**：

```properties
llm.provider=groq
llm.apiKey=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=mixtral-8x7b-32768
llm.baseUrl=
```

**推荐模型**：
- `mixtral-8x7b-32768` - Mixtral 8x7B (免费，速度快)
- `llama-3.1-70b-versatile` - Llama 3.1 70B (免费，效果好)
- `llama-3.1-8b-instant` - Llama 3.1 8B (免费，速度极快)

---

### 3. OpenAI

**优势**：
- ✅ GPT-4 系列模型效果最好
- ✅ 稳定性高，文档完善
- ✅ 支持 Function Calling

**注册**: [https://platform.openai.com](https://platform.openai.com)
**获取 API Key**: [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)

**配置示例**：

```properties
llm.provider=openai
llm.apiKey=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=gpt-4o-mini
llm.baseUrl=
```

**推荐模型**：
- `gpt-4o-mini` - GPT-4o Mini (便宜，$0.15/1M tokens)
- `gpt-4o` - GPT-4o (强大，$2.50/1M tokens)
- `gpt-3.5-turbo` - GPT-3.5 Turbo (最便宜，$0.50/1M tokens)

---

### 4. Claude (Anthropic)

**优势**：
- ✅ 推理能力强，擅长复杂任务
- ✅ 上下文长度大 (200K tokens)
- ✅ 安全性高

**注册**: [https://console.anthropic.com](https://console.anthropic.com)
**获取 API Key**: [https://console.anthropic.com/settings/keys](https://console.anthropic.com/settings/keys)

**配置示例**：

```properties
llm.provider=claude
llm.apiKey=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=claude-3-5-sonnet-20241022
llm.baseUrl=
```

**推荐模型**：
- `claude-3-5-sonnet-20241022` - Claude 3.5 Sonnet (最新，效果好)
- `claude-3-haiku-20240307` - Claude 3 Haiku (便宜，速度快)

---

### 5. Together AI

**优势**：
- ✅ 支持多种开源模型
- ✅ 价格便宜
- ✅ 支持自定义 fine-tuned 模型

**注册**: [https://api.together.xyz](https://api.together.xyz)
**获取 API Key**: [https://api.together.xyz/settings/api-keys](https://api.together.xyz/settings/api-keys)

**配置示例**：

```properties
llm.provider=together
llm.apiKey=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=meta-llama/Llama-3-8b-chat-hf
llm.baseUrl=
```

---

### 6. Local (本地模型)

**优势**：
- ✅ 完全私有，数据不离开设备
- ✅ 免费使用
- ✅ 低延迟

**前置条件**：需要自建 OpenAI 兼容的本地服务，如：
- [Ollama](https://ollama.ai) + OpenAI Proxy
- [LM Studio](https://lmstudio.ai)
- [llama.cpp](https://github.com/ggerganov/llama.cpp) + server

**配置示例（Ollama）**：

```properties
llm.provider=local
llm.apiKey=not-needed
llm.model=llama3.1:8b
llm.baseUrl=http://localhost:11434/v1
```

**Ollama 启动命令**：
```bash
# 安装 Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 拉取模型
ollama pull llama3.1:8b

# 启动 OpenAI 兼容服务
ollama serve
```

---

### 7. Mock (测试模式)

**优势**：
- ✅ 无需 API Key
- ✅ 适合开发测试
- ✅ 返回固定的模拟响应

**配置示例**：

```properties
llm.provider=mock
llm.apiKey=
llm.model=
llm.baseUrl=
```

**注意**: Mock 模式只返回预设的测试响应，不会真正理解用户输入。

---

## 配置字段说明

### `llm.provider`

指定使用的 LLM 提供商。

**可选值**：
- `openrouter` - OpenRouter (推荐)
- `groq` - Groq
- `openai` - OpenAI
- `claude` - Claude (Anthropic)
- `together` - Together AI
- `local` - 本地模型
- `mock` - 测试模式

### `llm.apiKey`

API 密钥，用于身份验证。

**获取方式**：
- 各平台的开发者控制台注册获取
- 免费注册即可获得试用额度
- **重要**: 不要将 API Key 提交到 Git！

### `llm.model`

指定使用的模型。

**可选**：
- 不填则使用默认模型
- 不同 provider 支持的模型不同
- 查看各平台文档获取完整模型列表

### `llm.baseUrl`

API 服务器地址。

**可选**：
- 通常不需要填写，使用默认地址
- 使用代理或自建服务时需要填写
- 本地模型必须填写 (如 `http://localhost:11434/v1`)

---

## 推荐配置方案

### 方案 1: 新手入门 (免费)

**OpenRouter + Gemini 2.0 Flash**

```properties
llm.provider=openrouter
llm.apiKey=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=google/gemini-2.0-flash-exp:free
llm.baseUrl=https://openrouter.ai/api/v1
```

**优势**: 完全免费，速度快，效果好

---

### 方案 2: 追求速度 (免费)

**Groq + Mixtral**

```properties
llm.provider=groq
llm.apiKey=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=mixtral-8x7b-32768
llm.baseUrl=
```

**优势**: 推理速度极快，免费额度大

---

### 方案 3: 追求效果 (付费)

**OpenAI GPT-4o-mini**

```properties
llm.provider=openai
llm.apiKey=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
llm.model=gpt-4o-mini
llm.baseUrl=
```

**优势**: 效果好，价格适中 ($0.15/1M tokens)

---

### 方案 4: 完全私有 (本地)

**Ollama + Llama 3.1**

```properties
llm.provider=local
llm.apiKey=not-needed
llm.model=llama3.1:8b
llm.baseUrl=http://localhost:11434/v1
```

**优势**: 数据完全私有，免费使用

---

## 常见问题

### Q1: 如何查看当前配置？

启动应用后，查看 logcat：

```bash
adb logcat | grep "LLM Config"
```

输出示例：
```
I/NanoApplication: LLM Config - Provider: openrouter, Model: google/gemini-2.0-flash-exp:free, BaseUrl: https://openrouter.ai/api/v1
```

---

### Q2: 配置修改后不生效？

**解决方法**：
1. 清理构建缓存：`./gradlew clean`
2. 重新构建：`./gradlew :app:installDebug`
3. 配置是在构建时注入的，必须重新编译

---

### Q3: API Key 泄露了怎么办？

**立即操作**：
1. 在对应平台删除旧的 API Key
2. 生成新的 API Key
3. 更新 `local.properties`
4. 检查 Git 历史，确保没有提交 API Key

**预防措施**：
- `local.properties` 已在 `.gitignore` 中
- 不要将 API Key 硬编码到代码中
- 不要截图包含 API Key 的配置文件

---

### Q4: 如何使用代理？

如果网络环境需要代理访问 OpenAI/Claude：

**方法 1**: 使用代理服务的 `baseUrl`
```properties
llm.baseUrl=https://your-proxy-domain.com/v1
```

**方法 2**: 通过 OpenRouter 中转
```properties
llm.provider=openrouter
llm.model=openai/gpt-4o-mini  # OpenRouter 支持多种模型
```

---

### Q5: 如何测试配置是否生效？

在应用中输入测试指令：

```
计算 2 + 3
```

如果配置正确，应该看到：
- IntentParser 解析出 `action: calculate`
- CalculatorAgent 返回结果 `5`
- 显示计算结果

如果使用 Mock 模式，会返回固定的测试响应。

---

## 成本估算

### 免费方案

| Provider | 模型 | 免费额度 | 限制 |
|----------|------|---------|------|
| OpenRouter | Gemini 2.0 Flash | 无限 | 速率限制 |
| Groq | Mixtral 8x7B | 14,400 请求/天 | 速率限制 |
| OpenRouter | Llama 3.1 8B | 无限 | 速率限制 |

### 付费方案估算

**场景**: 每天使用 100 次，每次平均 500 tokens

| Provider | 模型 | 每天成本 | 每月成本 |
|----------|------|---------|---------|
| OpenAI | GPT-4o-mini | $0.0075 | $0.225 |
| OpenAI | GPT-4o | $0.125 | $3.75 |
| Claude | Haiku | $0.0125 | $0.375 |
| Claude | Sonnet 3.5 | $0.15 | $4.50 |

**结论**: 使用 GPT-4o-mini 每月成本不到 1 元人民币。

---

## 安全建议

1. ✅ **不要提交 API Key 到 Git**
   - `local.properties` 已在 `.gitignore`
   - 定期检查 Git 历史

2. ✅ **使用最小权限的 API Key**
   - 只启用需要的权限
   - 设置支出限额

3. ✅ **定期轮换 API Key**
   - 建议每 3-6 个月更换一次
   - 发现泄露立即更换

4. ✅ **监控使用量**
   - 在平台控制台设置预算告警
   - 定期查看使用报告

5. ✅ **生产环境使用专用 Key**
   - 开发环境和生产环境分离
   - 使用不同的 API Key

---

## 调试技巧

### 查看 LLM 请求日志

```bash
adb logcat | grep -E "NanoLLM|IntentParser|LLMProvider"
```

### 查看意图解析结果

```bash
adb logcat | grep "IntentParser"
```

输出示例：
```
I/IntentParser: Parsed intent: search_flight
I/IntentParser: Broadcast capability: SEARCH
I/IntentParser: Confidence: 0.95
```

### 查看 Agent 执行日志

```bash
adb logcat | grep -E "SystemAgent|CtripAgent|ResponseAggregator"
```

---

## 相关文档

- [NanoAndroid 架构文档](../README.md)
- [Agent 开发指南](./AGENT_GUIDE.md)
- [A2UI 协议规范](./A2UI_SPEC.md)

---

## 获取帮助

如有问题，请：
1. 查看本文档的"常见问题"章节
2. 查看 [GitHub Issues](https://github.com/yourusername/nanoAndroid/issues)
3. 提交新的 Issue

---

**最后更新**: 2026-02-02
