package com.atlasshard.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        String corsOrigin,
        boolean cookieSecure,
        S3 s3,
        String uploadTemp) {
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {}
}
