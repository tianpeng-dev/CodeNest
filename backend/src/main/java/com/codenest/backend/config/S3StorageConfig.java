package com.codenest.backend.config;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfig {
  @Bean
  S3Client s3Client(StorageProperties properties) {
    var builder =
        S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyle()).build());

    if (StringUtils.hasText(properties.getEndpoint())) {
      builder.endpointOverride(URI.create(properties.getEndpoint()));
    }

    return builder.build();
  }

  private AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
    if (StringUtils.hasText(properties.getAccessKey())
        && StringUtils.hasText(properties.getSecretKey())) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }

    return AnonymousCredentialsProvider.create();
  }
}
