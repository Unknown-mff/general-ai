package ma.general.ai.service.llm

import ma.general.ai.constants.MimeType
import ma.general.ai.dto.FileDTO

interface ChatService {
    fun call(text: String): String
    fun call(text: String, fileBytes: ByteArray, mimeType: MimeType): String
    fun call(text: String, base64EncodedFile: String, mimeType: MimeType): String
    fun getJson(text: String, base64EncodedFile: String, mimeType: MimeType, schema: String?): String
    fun getJson(fileDTO: FileDTO): String
}