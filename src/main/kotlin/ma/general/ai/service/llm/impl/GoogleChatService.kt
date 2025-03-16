package ma.general.ai.service.llm.impl

import com.google.genai.Client
import com.google.genai.types.Blob
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import ma.general.ai.constants.MimeType
import ma.general.ai.dto.FileDTO
import ma.general.ai.service.llm.ChatService
import org.apache.http.HttpException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.Base64

@Service
class GoogleChatService(
    @Value("\${llm.api.google.key}") private val apiKey: String,
    @Value("\${llm.api.google.model}") private val model: String
) : ChatService {
    val client: Client by lazy { Client.builder().apiKey(apiKey).build() }

    override fun call(text: String): String {
        val textPart = getTextPart(text)
        val content = getContentFromParts(textPart)
        return getResponse(content, null)
    }

    override fun call(text: String, fileBytes: ByteArray, mimeType: MimeType): String =
        call(text, Base64.getEncoder().encodeToString(fileBytes), mimeType)

    override fun call(text: String, base64EncodedFile: String, mimeType: MimeType): String {
        return getResponse(getContentFromParts(getTextPart(text), getMediaPart(base64EncodedFile, mimeType)), null)
    }

    private fun getResponse(content: Content, config: GenerateContentConfig?): String = try {
        client.models.generateContent(model, content, config).text()
    } catch (e: IOException) {
        throw RuntimeException("Error during content generation (IO): ${e.message}", e)
    } catch (e: HttpException) {
        throw RuntimeException("Error during content generation (HTTP): ${e.message}", e)
    }

    override fun getJson(fileDto: FileDTO) = getJson(fileDto.text, fileDto.b64EFile, fileDto.mimeType, fileDto.schema)

    override fun getJson(
        text: String, base64EncodedFile: String, mimeType: MimeType, schema: String?
    ): String = getResponse(
        content = getContentFromParts(
            getTextPart(text), getMediaPart(base64EncodedFile, mimeType)
    ),
        config = GenerateContentConfig.builder().temperature(0.2f).topP(0.6f)
            .responseMimeType(MimeType.APPLICATION_JSON.mimeString)
            .apply { schema?.let { responseSchema(Schema.fromJson(it)) } }.build())
    private fun getContentFromParts(vararg parts: Part): Content = Content.builder().parts(parts.toList()).build()

    private fun getTextPart(text: String): Part = Part.builder().text(text).build()

    private fun getMediaPart(base64EncodedFile: String, mimeType: MimeType): Part {
        val blob = Blob.builder().data(base64EncodedFile).mimeType(mimeType.mimeString).build()
        return Part.builder().inlineData(blob).build()
    }


}