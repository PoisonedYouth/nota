package com.poisonedyouth.nota.validation

import com.poisonedyouth.nota.notes.CreateNoteDto
import com.poisonedyouth.nota.notes.ShareNoteDto
import com.poisonedyouth.nota.notes.UpdateNoteDto
import com.poisonedyouth.nota.user.LoginDto
import com.poisonedyouth.nota.user.RegisterDto
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import org.junit.jupiter.api.Test

class DtoValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `CreateNoteDto requires non-blank title and limits length`() {
        val tooLong = "a".repeat(256)
        validator.validate(CreateNoteDto("", "content"))
            .any { it.propertyPath.toString() == "title" } shouldBe true
        validator.validate(CreateNoteDto(tooLong, "content"))
            .any { it.propertyPath.toString() == "title" } shouldBe true
    }

    @Test
    fun `UpdateNoteDto validates id positive and title`() {
        val dto = UpdateNoteDto(0, "", "content", null)
        val violations = validator.validate(dto)
        violations.any { it.propertyPath.toString() == "id" } shouldBe true
        violations.any { it.propertyPath.toString() == "title" } shouldBe true
    }

    @Test
    fun `ShareNoteDto validates username and permission`() {
        validator.validate(ShareNoteDto("", "admin"))
            .size shouldBe 2
    }

    @Test
    fun `LoginDto requires username and password length`() {
        val v1 = validator.validate(LoginDto("", "short"))
        v1.any { it.propertyPath.toString() == "username" } shouldBe true
        v1.any { it.propertyPath.toString() == "password" } shouldBe true
    }

    @Test
    fun `RegisterDto requires username min length`() {
        val v = validator.validate(RegisterDto("ab"))
        v.any { it.propertyPath.toString() == "username" } shouldBe true
    }
}
