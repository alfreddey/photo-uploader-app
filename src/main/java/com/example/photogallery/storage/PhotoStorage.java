package com.example.photogallery.storage;

/**
 * Where photo bytes live and how the browser reaches them. Two implementations:
 * {@link LocalDiskStorage} (dev) and {@link S3Storage} (production, fronted by
 * CloudFront). The app server always does the write — the production S3 bucket
 * is fully private, so CloudFront (GET/HEAD only via OAC) can never receive an
 * upload.
 */
public interface PhotoStorage {

    /** Persist {@code data} under {@code key}. */
    void store(String key, byte[] data, String contentType);

    /** Remove the object at {@code key}; a no-op if it is already gone. */
    void delete(String key);

    /** Public URL the browser loads the image from. */
    String url(String key);
}
