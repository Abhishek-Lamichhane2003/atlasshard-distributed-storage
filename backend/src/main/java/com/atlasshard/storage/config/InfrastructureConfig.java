package com.atlasshard.storage.config;

import java.net.URI;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class InfrastructureConfig {
    @Bean
    S3Client s3Client(AppProperties props) {
        return S3Client.builder()
            .endpointOverride(URI.create(props.s3().endpoint()))
            .region(Region.of(props.s3().region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.s3().accessKey(), props.s3().secretKey())))
            .forcePathStyle(true)
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.events.mode", havingValue = "kafka", matchIfMissing = true)
    NewTopic storageEventsTopic() {
        return TopicBuilder.name("storage-events").partitions(3).replicas(1).build();
    }
}
