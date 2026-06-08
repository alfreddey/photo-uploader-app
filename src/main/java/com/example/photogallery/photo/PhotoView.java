package com.example.photogallery.photo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** A photo prepared for rendering: storage key already resolved to a URL. */
public record PhotoView(Long id, String url, String description,
                        String originalFilename, Instant createdAt) {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    /** Human-friendly date for the template (Instant can't be pattern-formatted directly). */
    public String getDisplayDate() {
        return createdAt == null ? "" : DATE.format(createdAt);
    }
}
