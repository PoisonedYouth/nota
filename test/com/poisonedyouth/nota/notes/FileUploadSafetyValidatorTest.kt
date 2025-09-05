package com.poisonedyouth.nota.notes

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class FileUploadSafetyValidatorTest {

    private val props = FileUploadSafetyProperties(
        maxSizeBytes = 1024 * 1024, // 1MB
        allowedExtensions = listOf("txt", "png", "jpg", "jpeg", "gif", "pdf"),
        allowedMimeTypes = listOf(
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/gif",
            "application/pdf",
        ),
    )
    private val validator = FileUploadSafetyValidator(props)

    @Test
    fun `rejects empty file`() {
        val file = MockMultipartFile("file", "test.txt", "text/plain", byteArrayOf())
        shouldThrow<IllegalStateException> { validator.validate(file) }
    }

    @Test
    fun `accepts small text file`() {
        val file = MockMultipartFile("file", "notes.txt", "text/plain", "Hello".toByteArray())
        validator.validate(file)
        validator.sanitizeFilename(file.originalFilename) shouldBe "notes.txt"
    }

    @Test
    fun `rejects executable disguised as txt`() {
        val fake = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()) // ELF
        val file = MockMultipartFile("file", "evil.txt", "text/plain", fake)
        shouldThrow<UnsupportedOperationException> { validator.validate(file) }
    }

    @Test
    fun `rejects disallowed extension`() {
        val file = MockMultipartFile("file", "script.sh", "text/plain", "echo".toByteArray())
        shouldThrow<UnsupportedOperationException> { validator.validate(file) }
    }

    @Test
    fun `sanitizes filename to prevent path traversal`() {
        val sanitized = validator.sanitizeFilename("../..\\/etc/passwd")
        sanitized.shouldBe("passwd")
    }
}
