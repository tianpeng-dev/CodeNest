package com.codenest.backend.upload;

import com.codenest.backend.config.StorageProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService implements StorageService {
  private final S3Client s3Client;
  private final StorageProperties properties;

  public S3StorageService(S3Client s3Client, StorageProperties properties) {
    this.s3Client = s3Client;
    this.properties = properties;
  }

  @Override
  public String upload(String objectKey, byte[] bytes, String contentType) {
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(properties.getBucket())
            .key(objectKey)
            .contentType(contentType)
            .contentLength((long) bytes.length)
            .build();
    s3Client.putObject(request, RequestBody.fromBytes(bytes));
    return objectUrl(objectKey);
  }

  private String objectUrl(String objectKey) {
    if (StringUtils.hasText(properties.getPublicBaseUrl())) {
      return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
    }

    String encodedKey = encodePath(objectKey);
    if (StringUtils.hasText(properties.getEndpoint())) {
      return trimTrailingSlash(properties.getEndpoint())
          + "/"
          + properties.getBucket()
          + "/"
          + encodedKey;
    }

    if ("auto".equalsIgnoreCase(properties.getRegion())) {
      return "https://" + properties.getBucket() + ".s3.amazonaws.com/" + encodedKey;
    }

    return "https://"
        + properties.getBucket()
        + ".s3."
        + properties.getRegion()
        + ".amazonaws.com/"
        + encodedKey;
  }

  private String trimTrailingSlash(String value) {
    return value.replaceAll("/+$", "");
  }

  private String encodePath(String path) {
    return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
  }
}
