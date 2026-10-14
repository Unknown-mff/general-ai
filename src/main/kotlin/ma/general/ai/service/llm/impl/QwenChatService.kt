package ma.general.ai.service.llm.impl

import ma.general.ai.constants.MimeType
import ma.general.ai.dto.FileDTO
import ma.general.ai.service.llm.ChatService
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Base64
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper

@Service("qwenChatService")
class QwenChatService(
    @Value("\${llm.api.qwen.url}") private val url: String,
    @Value("\${llm.api.qwen.key}") private val apiKey: String,
    @Value("\${llm.api.qwen.model}") private val model: String,
    private val objectMapper: ObjectMapper
) : ChatService {

    private val logger = LoggerFactory.getLogger(QwenChatService::class.java)

    override fun call(text: String): String {
        return cleanJsonResponse(queryQwen(text))
    }

    override fun call(text: String, fileBytes: ByteArray, mimeType: MimeType): String {
        val extractedText = extractText(fileBytes)
        val combinedPrompt = "$text\n\n=== TEXTE DU DOCUMENT ===\n$extractedText"
        return queryQwen(combinedPrompt)
    }

    override fun call(text: String, base64EncodedFile: String, mimeType: MimeType): String {
        val bytes = Base64.getDecoder().decode(base64EncodedFile)
        return call(text, bytes, mimeType)
    }

    override fun getJson(fileDTO: FileDTO): String {
        return getJson(fileDTO.text, fileDTO.b64EFile, fileDTO.mimeType, fileDTO.schema)
    }

    override fun getJson(
        text: String, base64EncodedFile: String, mimeType: MimeType, schema: String?
    ): String {
        val bytes = try {
            Base64.getDecoder().decode(base64EncodedFile)
        } catch (e: Exception) {
            logger.error("Erreur lors du décodage Base64: ${e.message}")
            ByteArray(0)
        }

        val extractedText = extractText(bytes)
        
        // Build the system instructions/prompt
        val promptWithSchema = StringBuilder()
        promptWithSchema.append(text)
        if (!schema.isNullOrBlank()) {
            promptWithSchema.append("\n\nLe résultat doit respecter scrupuleusement le format JSON suivant :\n$schema\n")
        }
        promptWithSchema.append("\nNe retourne AUCUNE explication, aucun texte d'introduction ni de conclusion. Retourne UNIQUEMENT le bloc de code JSON brut valide.")
        promptWithSchema.append("\n\n=== TEXTE DU CV ===\n$extractedText")

        val rawResponse = queryQwen(promptWithSchema.toString())
        return cleanJsonResponse(rawResponse)
    }

    private fun extractText(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return try {
            val tika = Tika()
            val text = tika.parseToString(ByteArrayInputStream(bytes))
            logger.info("Extraction Tika réussie. Taille du texte extrait: ${text.length} caractères.")
            text
        } catch (e: Exception) {
            logger.error("Erreur lors de l'extraction de texte via Tika: ${e.message}", e)
            ""
        }
    }

    private fun queryQwen(prompt: String): String {
        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to "You are a JSON-only API. You MUST respond with ONLY a valid JSON array or object. Do NOT write any introduction, explanation, conclusion, or conversational text. Your entire response must start with '[' or '{' and end with ']' or '}'. No markdown, no code blocks, no text before or after the JSON."
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "temperature" to 0.1
        )

        logger.info("Envoi de la requête à Qwen (${url}/chat/completions) avec le modèle $model")
        
        val jsonRequest = objectMapper.writeValueAsString(requestBody)

        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("$url/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequest))
            .build()
            
        val responseString = try {
            val response = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build()
                .send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Erreur de communication avec le service d'IA Qwen (status ${response.statusCode()}): ${response.body()}")
            }
            response.body()
        } catch (e: Exception) {
            logger.error("Erreur lors de l'appel à Qwen: ${e.message}", e)
            throw RuntimeException("Erreur de communication avec le service d'IA Qwen: ${e.message}", e)
        }

        if (responseString.isNullOrBlank()) {
            throw RuntimeException("Réponse vide de la part de Qwen")
        }

        val response = objectMapper.readValue(responseString, Map::class.java)

        val choices = response["choices"] as? List<*>
        if (choices.isNullOrEmpty()) {
            logger.warn("Réponse Qwen sans choix: $response")
            return ""
        }

        val firstChoice = choices[0] as? Map<*, *>
        val message = firstChoice?.get("message") as? Map<*, *>
        val content = message?.get("content") as? String

        return content ?: ""
    }

    private fun cleanJsonResponse(response: String): String {
        var cleaned = response.trim()
        
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substringAfter("```json").substringBeforeLast("```")
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substringAfter("```").substringBeforeLast("```")
        }
        
        cleaned = cleaned.trim()
        
        // If it still doesn't look like valid JSON (doesn't start with [ or {), try to extract it
        if (!cleaned.startsWith("[") && !cleaned.startsWith("{")) {
            val arrayStart = cleaned.indexOf('[')
            val arrayEnd = cleaned.lastIndexOf(']')
            
            val objStart = cleaned.indexOf('{')
            val objEnd = cleaned.lastIndexOf('}')
            
            // Heuristic to find the outermost JSON structure
            if (arrayStart != -1 && arrayEnd != -1 && (objStart == -1 || arrayStart < objStart)) {
                cleaned = cleaned.substring(arrayStart, arrayEnd + 1)
            } else if (objStart != -1 && objEnd != -1) {
                cleaned = cleaned.substring(objStart, objEnd + 1)
            }
        }
        
        return cleaned.trim()
    }
}
