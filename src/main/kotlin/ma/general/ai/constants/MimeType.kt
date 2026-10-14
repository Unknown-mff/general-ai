package ma.general.ai.constants

import com.fasterxml.jackson.annotation.JsonCreator
import org.apache.tika.Tika
import java.util.*

enum class MimeType(
    val mimeString: String,
    val extensions: List<String> = emptyList()
) {


    // Text Types
    TEXT_PLAIN("text/plain", listOf("txt")),
    TEXT_HTML("text/html", listOf("html", "htm")),
    TEXT_CSS("text/css", listOf("css")),
    TEXT_JAVASCRIPT("text/javascript", listOf("js")),
    TEXT_CSV("text/csv", listOf("csv")),
    TEXT_XML("text/xml", listOf("xml")),

    // Image Types
    IMAGE_JPEG("image/jpeg", listOf("jpeg", "jpg")),
    IMAGE_PNG("image/png", listOf("png")),
    IMAGE_GIF("image/gif", listOf("gif")),
    IMAGE_BMP("image/bmp", listOf("bmp")),
    IMAGE_WEBP("image/webp", listOf("webp")),
    IMAGE_SVG("image/svg+xml", listOf("svg")),

    // Audio Types
    AUDIO_MPEG("audio/mpeg", listOf("mp3")),
    AUDIO_WAV("audio/wav", listOf("wav")),
    AUDIO_OGG("audio/ogg", listOf("ogg")),
    AUDIO_AAC("audio/aac", listOf("aac")),

    // Video Types
    VIDEO_MP4("video/mp4", listOf("mp4")),
    VIDEO_MPEG("video/mpeg", listOf("mpeg", "mpg")),
    VIDEO_WEBM("video/webm", listOf("webm")),
    VIDEO_OGG("video/ogg", listOf("ogv")),

    // Application Types
    APPLICATION_JSON("application/json", listOf("json")),
    APPLICATION_PDF("application/pdf", listOf("pdf")),
    APPLICATION_ZIP("application/zip", listOf("zip")),
    APPLICATION_GZIP("application/gzip", listOf("gz")),
    APPLICATION_OCTET_STREAM("application/octet-stream"), // Generic binary data
    APPLICATION_MSWORD("application/msword", listOf("doc")),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        listOf("docx")
    ),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        listOf("xlsx")
    ),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        listOf("pptx")
    ),
    APPLICATION_X_SHOCKWAVE_FLASH("application/x-shockwave-flash", listOf("swf")),
    APPLICATION_JAVA_ARCHIVE("application/java-archive", listOf("jar")),
    APPLICATION_POSTSCRIPT("application/postscript", listOf("ps")),

    // Font Types
    FONT_WOFF("font/woff", listOf("woff")),
    FONT_WOFF2("font/woff2", listOf("woff2")),
    FONT_TTF("font/ttf", listOf("ttf")),
    FONT_OTF("font/otf", listOf("otf"));

    fun matchesExtension(extension: String?): Boolean =
        extension?.removePrefix(".")?.lowercase()?.let { ext -> extensions.contains(ext) } == true


    companion object {
        @JsonCreator
        @JvmStatic
        fun fromJson(value: String?): MimeType {
            if (value == null) return APPLICATION_PDF
            // Try matching by enum name first (e.g., APPLICATION_PDF)
            val byName = entries.find { it.name.equals(value, ignoreCase = true) }
            if (byName != null) return byName
            // Fall back to matching by MIME string (e.g., application/pdf)
            return entries.find { it.mimeString.equals(value, ignoreCase = true) }
                ?: APPLICATION_PDF
        }

        fun fromString(mimeString: String?): MimeType? {
            return entries.find { it.mimeString.equals(mimeString, ignoreCase = true) }
        }

        fun fromExtension(extension: String?): MimeType? {
            if (extension.isNullOrEmpty()) return null
            val ext = extension.removePrefix(".").lowercase()
            return MimeType.entries.find { it.extensions.contains(ext) }
        }

        fun fromBase64(b64: String): MimeType? {
            return try {
                val decodedBytes = Base64.getDecoder().decode(b64)

                val mimeTypeString = Tika().detect(decodedBytes)
                fromString(mimeTypeString)
            } catch (e: Exception) {
                null
            }
        }
    }
}