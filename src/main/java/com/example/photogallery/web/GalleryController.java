package com.example.photogallery.web;

import com.example.photogallery.photo.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GalleryController {

    private final PhotoService photoService;

    public GalleryController(PhotoService photoService) {
        this.photoService = photoService;
    }

    // GET / is also the ALB blue/green health check — it must return 2xx.
    @GetMapping("/")
    public String gallery(Model model) {
        model.addAttribute("photos", photoService.listPhotos());
        return "gallery";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "description", required = false) String description,
                         RedirectAttributes redirectAttributes) {
        try {
            photoService.upload(file, description);
            redirectAttributes.addFlashAttribute("flash", "Your photo joined the gallery.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/upload";
        }
    }

    @PostMapping("/photos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        photoService.delete(id);
        redirectAttributes.addFlashAttribute("flash", "Photo removed.");
        return "redirect:/";
    }
}
