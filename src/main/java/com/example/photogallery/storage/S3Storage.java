package com.example.photogallery.storage;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Production storage: uploads objects to the private S3 image bucket using the
 * ECS task role (DefaultCredentialsProvider), and returns CloudFront URLs for
 * reads. The bucket blocks all public access and only trusts CloudFront's OAC,
 * so reads must go through the distribution domain — never s3.amazonaws.com.
 */
public class S3Storage implements PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(S3Storage.class);

    private final S3Client s3;
    private final String bucket;
    private final String cloudFrontDomain;

    public S3Storage(String bucket, String cloudFrontDomain, String region) {
        this.bucket = bucket;
        this.cloudFrontDomain = stripScheme(cloudFrontDomain);
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        log.info("Photo storage: S3 bucket={} served via CloudFront={} (region {})",
                bucket, this.cloudFrontDomain, region);
    }

    @Override
    public void store(String key, byte[] data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String url(String key) {
        return "https://" + cloudFrontDomain + "/" + key;
    }

    private static String stripScheme(String domain) {
        if (domain == null) {
            return null;
        }
        return domain.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    @PreDestroy
    public void close() {
        s3.close();
    }
}
