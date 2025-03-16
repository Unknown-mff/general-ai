package ma.general.ai.controllers

import ma.general.ai.dto.FileDTO
import ma.general.ai.service.llm.ChatService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai/api/v1/extract")
class DataExtractionController(val chatService: ChatService) {

    @PostMapping
    fun extractData(@RequestBody fileDTO: FileDTO): String {
        return chatService.getJson(fileDTO);
    }

}