package com.poisonedyouth.nota.notes

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.Locale

@Component
@ConfigurationProperties(prefix = "nota.upload")
data class FileUploadSafetyProperties(
    val maxSizeBytes: Long = 10L * 1024 * 1024, // 10 MB default
    val allowedExtensions: List<String> = listOf("txt", "md", "pdf", "png", "jpg", "jpeg", "gif"),
    val allowedMimeTypes: List<String> = listOf(
        "text/plain",
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/gif",
    ),
)

@Component
class FileUploadSafetyValidator(
    private val props: FileUploadSafetyProperties,
) {
    private val logger = LoggerFactory.getLogger(FileUploadSafetyValidator::class.java)

    fun validate(file: MultipartFile) {
        check(!file.isEmpty) { "File must not be empty" }
        val originalName = sanitizeFilename(file.originalFilename)
        check(originalName.isNotBlank()) { "Filename is required" }

        // Size check
        if (file.size > props.maxSizeBytes) {
            throw IllegalArgumentException("File too large. Max: ${props.maxSizeBytes} bytes")
        }

        // Extension check
        val ext = originalName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (ext.isBlank() || !props.allowedExtensions.any { it.equals(ext, ignoreCase = true) }) {
            throw UnsupportedOperationException("File extension '$ext' not allowed")
        }

        // Content type check (header + basic sniffing fallback)
        val headerType = (file.contentType ?: "").lowercase(Locale.getDefault())
        val headerAllowed = props.allowedMimeTypes.any { it.equals(headerType, ignoreCase = true) }
        val magicAllowed = isMagicAllowed(file.bytes)
        if (!(headerAllowed && magicAllowed)) {
            logger.warn(
                "Rejected upload due to content-type mismatch: header='{}', magicAllowed={} name='{}' size={}",
                headerType,
                magicAllowed,
                originalName,
                file.size,
            )
            throw UnsupportedOperationException("File content type not allowed")
        }
    }

    fun sanitizeFilename(name: String?): String {
        val safe = name?.replace("\\", "/")?.substringAfterLast('/')?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: ""
        // Prevent hidden files and path tricks
        return safe.trim('.').take(255)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun isMagicAllowed(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        // Very small, conservative magic checks for common types we allow
        return when {
            // PNG
            bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() -> true
            // JPEG
            bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> true
            // GIF
            bytes.size >= 6 &&
                bytes[0] == 'G'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() -> true
            // PDF
            bytes.size >= 5 &&
                bytes[0] == '%'.code.toByte() &&
                bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'D'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() -> true
            // Plain text heuristic: limit ASCII proportion
            else -> bytes.take(64).all { it == 9.toByte() || it == 10.toByte() || it == 13.toByte() || (it in 32..126) }
        }
    }
}
