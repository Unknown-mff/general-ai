package ma.general.ai.controllers

import ma.general.ai.service.llm.ChatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import org.springframework.http.MediaType

@RestController
@RequestMapping("/ai/api/v1")
class AiProcessController(
    val chatService: ChatService
) {

    private val logger = LoggerFactory.getLogger(AiProcessController::class.java)

    @PostMapping("/extractOfferFormattedDescription", consumes = [MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE, "*/*"])
    fun extractOfferFormattedDescription(@RequestBody prompt: String): String {
        logger.info("Received request to generate offer description. Prompt length: ${prompt.length}")
        return cleanResponse(chatService.call(prompt))
    }

    @PostMapping("/generateInterviewQuestions", consumes = [MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE, "*/*"])
    fun generateInterviewQuestions(@RequestBody prompt: String): String {
        logger.info("Received request to generate interview questions. Prompt length: ${prompt.length}")
        return cleanResponse(chatService.call(prompt))
    }

    @PostMapping("/evaluationInterview", consumes = [MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE, "*/*"])
    fun evaluationInterview(@RequestBody prompt: String): String {
        logger.info("Received request to evaluate interview answers. Prompt length: ${prompt.length}")
        return cleanResponse(chatService.call(prompt))
    }

    private fun cleanResponse(response: String): String {
        var cleaned = response.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("\n")
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substringBeforeLast("```")
            }
        }
        return cleaned.trim()
    }
}
