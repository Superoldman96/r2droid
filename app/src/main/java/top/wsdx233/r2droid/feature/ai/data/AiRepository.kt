package top.wsdx233.r2droid.feature.ai.data

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.StreamOptions
import com.aallam.openai.api.chat.ChatMessage as ApiChatMessage
import com.aallam.openai.api.chat.ChatRole as ApiChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor() {

    private var openAI: OpenAI? = null
    private var currentProviderId: String? = null
    private var currentProvider: AiProvider? = null

    fun configure(provider: AiProvider) {
        currentProvider = provider
        if (currentProviderId == provider.id && openAI != null) return
        currentProviderId = provider.id
        openAI = OpenAI(
            OpenAIConfig(
                token = provider.apiKey,
                host = OpenAIHost(baseUrl = provider.baseUrl.trimEnd('/') + "/")
            )
        )
    }

    fun streamChat(
        messages: List<ChatMessage>,
        modelName: String,
        systemPrompt: String,
        useResponsesApi: Boolean = false,
        thinkingLevel: ThinkingLevel = ThinkingLevel.Auto
    ): Flow<String> {
        if (useResponsesApi) {
            val provider = currentProvider ?: throw IllegalStateException("AI provider not configured")
            return streamResponsesApi(provider, messages, modelName, systemPrompt, thinkingLevel)
        }

        val client = openAI ?: throw IllegalStateException("AI provider not configured")

        val apiMessages = buildList {
            add(ApiChatMessage(role = ApiChatRole.System, content = systemPrompt))
            for (msg in messages) {
                val role = when (msg.role) {
                    ChatRole.User, ChatRole.ExecutionResult -> ApiChatRole.User
                    ChatRole.Assistant -> ApiChatRole.Assistant
                    ChatRole.System -> ApiChatRole.System
                }
                add(ApiChatMessage(role = role, content = msg.content))
            }
        }

        val request = ChatCompletionRequest(
            model = ModelId(modelName),
            messages = apiMessages,
            streamOptions = StreamOptions(includeUsage = true)
        )

        return client.chatCompletions(request)
            .mapNotNull { chunk ->
                chunk.choices.firstOrNull()?.delta?.content
            }
            .flowOn(Dispatchers.IO)
    }

    private fun streamResponsesApi(
        provider: AiProvider,
        messages: List<ChatMessage>,
        modelName: String,
        systemPrompt: String,
        thinkingLevel: ThinkingLevel
    ): Flow<String> = flow {
        val endpoint = provider.baseUrl.trimEnd('/') + "/responses"
        val payload = buildResponsesPayload(messages, modelName, systemPrompt, thinkingLevel)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doInput = true
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${provider.apiKey.trim()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
        }

        try {
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Response API failed (${responseCode}): $errorBody")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (!line.startsWith("data:")) return@forEach
                    val payloadLine = line.removePrefix("data:").trim()
                    if (payloadLine == "[DONE]") return@forEach
                    val delta = extractResponseDelta(payloadLine)
                    if (delta.isNotBlank()) {
                        emit(delta)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildResponsesPayload(
        messages: List<ChatMessage>,
        modelName: String,
        systemPrompt: String,
        thinkingLevel: ThinkingLevel
    ): String {
        val input = JSONArray()
        input.put(
            JSONObject()
                .put("role", "system")
                .put("content", JSONArray().put(JSONObject().put("type", "input_text").put("text", systemPrompt)))
        )

        messages.forEach { msg ->
            val role = when (msg.role) {
                ChatRole.User, ChatRole.ExecutionResult -> "user"
                ChatRole.Assistant -> "assistant"
                ChatRole.System -> "system"
            }
            val contentType = if (role == "assistant") "output_text" else "input_text"
            input.put(
                JSONObject()
                    .put("role", role)
                    .put("content", JSONArray().put(JSONObject().put("type", contentType).put("text", msg.content)))
            )
        }

        val root = JSONObject()
            .put("model", modelName)
            .put("input", input)
            .put("stream", true)
        thinkingLevel.apiEffort?.let { effort ->
            root.put("reasoning", JSONObject().put("effort", effort))
        }
        return root.toString()
    }

    private fun extractResponseDelta(eventPayload: String): String {
        if (eventPayload.isBlank()) return ""
        val json = JSONObject(eventPayload)
        return when {
            json.optString("type") == "response.output_text.delta" -> json.optString("delta", "")
            json.has("delta") -> json.optString("delta", "")
            json.optString("type") == "response.completed" -> {
                extractResponsesText(json.optJSONObject("response")?.toString().orEmpty())
            }
            else -> ""
        }
    }

    private fun extractResponsesText(responseBody: String): String {
        if (responseBody.isBlank()) return ""
        val json = JSONObject(responseBody)

        val direct = json.optString("output_text", "")
        if (direct.isNotBlank()) return direct

        val output = json.optJSONArray("output") ?: return ""
        val builder = StringBuilder()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val text = when {
                    part.optString("type") == "output_text" -> part.optString("text", "")
                    else -> part.optString("text", "")
                }
                if (text.isNotBlank()) {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append(text)
                }
            }
        }
        return builder.toString()
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> {
        val client = OpenAI(
            OpenAIConfig(
                token = apiKey,
                host = OpenAIHost(baseUrl = baseUrl.trimEnd('/') + "/")
            )
        )
        return client.models().map { it.id.id }.sorted()
    }

    fun resetClient() {
        openAI = null
        currentProviderId = null
        currentProvider = null
    }
}
