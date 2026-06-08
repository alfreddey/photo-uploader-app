package com.example.photogallery.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dev storage: writes files under a local directory and serves them back
 * through the app's {@code /media/**} resource handler (see WebConfig). Stands
 * in for the S3 + CloudFront pair so the app runs with no AWS dependency.
 */
public class LocalDiskStorage implements PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskStorage.class);

    private final Path root;

    public LocalDiskStorage(String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create local storage dir " + root, e);
        }
        log.info("Photo storage: local disk at {}", root);
    }

    @Override
    public void store(String key, byte[] data, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + key, e);
        }
    }

    @Override
    public String url(String key) {
        return "/media/" + key;
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Key escapes storage root: " + key);
        }
        return target;
    }
}
