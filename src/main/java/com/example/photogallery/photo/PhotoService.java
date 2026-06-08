package com.example.photogallery.photo;

import com.example.photogallery.storage.PhotoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");

    private final PhotoRepository repository;
    private final PhotoStorage storage;

    public PhotoService(PhotoRepository repository, PhotoStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<PhotoView> listPhotos() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> new PhotoView(
                        p.getId(),
                        storage.url(p.getObjectKey()),
                        p.getDescription(),
                        p.getOriginalFilename(),
                        p.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void upload(MultipartFile file, String description) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image to upload.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Upload a JPEG, PNG, WebP, GIF, or AVIF image.");
        }

        String key = "photos/" + UUID.randomUUID() + extensionFor(contentType);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }

        // Write the bytes first; only record metadata if the object landed.
        storage.store(key, bytes, contentType);
        repository.save(new Photo(key, normalize(description),
                file.getOriginalFilename(), contentType, file.getSize()));
        log.info("Stored photo {} ({} bytes)", key, file.getSize());
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id).ifPresent(photo -> {
            storage.delete(photo.getObjectKey());
            repository.delete(photo);
            log.info("Deleted photo {} ({})", id, photo.getObjectKey());
        });
    }

    private static String normalize(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/avif" -> ".avif";
            default -> "";
        };
    }
}
