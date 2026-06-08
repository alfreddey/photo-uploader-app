package com.example.photogallery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves locally-stored photos at {@code /media/**} (dev only). In production
 * images are served by CloudFront, so this path is simply never requested.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String localDir;

    public WebConfig(@Value("${STORAGE_LOCAL_DIR:uploads}") String localDir) {
        this.localDir = localDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(localDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
