package ma.general.ai.controllers

import ma.general.ai.dto.FileDTO
import ma.general.ai.service.llm.ChatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai/api/v1/extract")
class DataExtractionController(
    @Qualifier("ollamaChatService") val chatService: ChatService
) {

    private val logger = LoggerFactory.getLogger(DataExtractionController::class.java)

    @PostMapping
    fun extractData(@RequestBody fileDTO: FileDTO): String {
        logger.info("Received CV extraction request. Base64 length: ${fileDTO.b64EFile.length}, mimeType: ${fileDTO.mimeType}")
        try {
            val result = chatService.getJson(fileDTO)
            logger.info("Successfully processed CV extraction request. JSON result length: ${result.length}")
            return result
        } catch (e: Exception) {
            logger.error("Failed to process CV extraction in controller: ${e.message}", e)
            throw e
        }
    }

}