package com.example.photogallery.config;

import com.example.photogallery.storage.LocalDiskStorage;
import com.example.photogallery.storage.PhotoStorage;
import com.example.photogallery.storage.S3Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Picks the storage backend. {@code STORAGE_DRIVER} (local|s3) is honoured when
 * set; otherwise the presence of {@code IMAGE_BUCKET} decides — so the ECS task
 * (which sets IMAGE_BUCKET but not STORAGE_DRIVER) automatically uses S3, while
 * docker-compose defaults to local disk.
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public PhotoStorage photoStorage(
            @Value("${STORAGE_DRIVER:}") String driver,
            @Value("${IMAGE_BUCKET:}") String bucket,
            @Value("${CLOUDFRONT_DOMAIN:}") String cloudFrontDomain,
            @Value("${STORAGE_LOCAL_DIR:uploads}") String localDir,
            @Value("${AWS_REGION:${AWS_DEFAULT_REGION:eu-north-1}}") String region) {

        String resolved = !driver.isBlank()
                ? driver.toLowerCase()
                : (bucket.isBlank() ? "local" : "s3");
        log.info("Resolved storage driver: {}", resolved);

        if ("s3".equals(resolved)) {
            if (bucket.isBlank()) {
                throw new IllegalStateException("STORAGE_DRIVER=s3 but IMAGE_BUCKET is not set");
            }
            if (cloudFrontDomain.isBlank()) {
                throw new IllegalStateException("STORAGE_DRIVER=s3 but CLOUDFRONT_DOMAIN is not set");
            }
            return new S3Storage(bucket, cloudFrontDomain, region);
        }
        return new LocalDiskStorage(localDir);
    }
}
