package com.poisonedyouth.nota.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest,
        model: Model,
        redirectAttributes: RedirectAttributes,
    ): String {
        val errorMessage = "File too large! Maximum file size is 25MB."

        // Check if this is an HTMX request
        val htmxRequest = request.getHeader("HX-Request")

        return if (htmxRequest != null) {
            // HTMX request - return error fragment
            model.addAttribute("error", errorMessage)
            "notes/fragments :: attachment-error"
        } else {
            // Regular request - redirect with flash attribute
            redirectAttributes.addFlashAttribute("error", errorMessage)
            "redirect:/notes"
        }
    }
}
