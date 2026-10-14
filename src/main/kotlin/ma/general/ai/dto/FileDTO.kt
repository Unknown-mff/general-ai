package ma.general.ai.dto

import ma.general.ai.constants.MimeType

data class FileDTO(
    val text: String,
    val b64EFile: String,
    val mimeType: MimeType = MimeType.APPLICATION_PDF,
    val schema: String? = null,
)

