package ma.general.ai.service.llm.impl

import ma.general.ai.constants.MimeType
import ma.general.ai.dto.FileDTO
import ma.general.ai.service.llm.ChatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class FailoverChatService(
    @Qualifier("qwenChatService") private val qwenService: ChatService,
    @Qualifier("ollamaChatService") private val groqService: ChatService,
    @Qualifier("googleChatService") private val googleService: ChatService
) : ChatService {

    private val logger = LoggerFactory.getLogger(FailoverChatService::class.java)

    // La liste ordonnée des services à tester (Gemini d'abord, puis Groq, puis Qwen)
    private val services = listOf(googleService, groqService, qwenService)

    private fun <T> executeWithFailover(operationName: String, operation: (ChatService) -> T): T {
        var lastException: Exception? = null
        for ((index, service) in services.withIndex()) {
            try {
                logger.info("Tentative de l'opération '$operationName' avec le service IA #${index + 1} (${service.javaClass.simpleName})")
                val result = operation(service)
                // Vérifier que la réponse n'est pas vide et qu'elle ressemble à du JSON
                if (result is String) {
                    val trimmed = result.trim()
                    if (trimmed.isBlank()) {
                        throw RuntimeException("Le service a retourné une réponse vide.")
                    }
                    if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
                        throw RuntimeException("La réponse n'est pas un JSON valide (ne commence ni par { ni par [). Texte reçu : ${trimmed.take(50)}...")
                    }
                }
                
                logger.info("Succès de l'opération '$operationName' avec le service IA #${index + 1}")
                return result
            } catch (e: Exception) {
                logger.warn("Échec avec le service IA #${index + 1} (${service.javaClass.simpleName}): ${e.message}. Passage au service suivant...")
                lastException = e
            }
        }
        
        val errorMsg = "Tous les services IA ont échoué pour l'opération '$operationName'. Dernière erreur: ${lastException?.message}"
        logger.error(errorMsg, lastException)
        throw RuntimeException(errorMsg, lastException)
    }

    override fun call(text: String): String =
        executeWithFailover("call(text)") { it.call(text) }

    override fun call(text: String, fileBytes: ByteArray, mimeType: MimeType): String =
        executeWithFailover("call(text, bytes, mimeType)") { it.call(text, fileBytes, mimeType) }

    override fun call(text: String, base64EncodedFile: String, mimeType: MimeType): String =
        executeWithFailover("call(text, base64, mimeType)") { it.call(text, base64EncodedFile, mimeType) }

    override fun getJson(text: String, base64EncodedFile: String, mimeType: MimeType, schema: String?): String =
        executeWithFailover("getJson(text, base64, mimeType, schema)") { it.getJson(text, base64EncodedFile, mimeType, schema) }

    override fun getJson(fileDTO: FileDTO): String =
        executeWithFailover("getJson(fileDTO)") { it.getJson(fileDTO) }
}
