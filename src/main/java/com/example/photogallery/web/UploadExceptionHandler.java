package com.example.photogallery.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Multipart size limits are enforced before the controller method runs, so the
 * resulting exception is handled here and turned into a friendly redirect.
 */
@ControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "That image is too large (10 MB max).");
        return "redirect:/upload";
    }
}
