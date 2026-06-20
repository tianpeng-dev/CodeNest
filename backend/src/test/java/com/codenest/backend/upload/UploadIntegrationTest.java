package com.codenest.backend.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:upload_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json",
      "codenest.storage.bucket=test-bucket",
      "codenest.storage.public-base-url=https://cdn.example.com"
    })
class UploadIntegrationTest {
  private static final String CLERK_ID = "clerk_upload_owner";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FakeStorageService storageService;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM file_objects");
    jdbcTemplate.update("DELETE FROM users");
    storageService.reset();
  }

  @Test
  void authenticatedImageUploadStoresMetadataAndReturnsUrl() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "png-bytes".getBytes());

    mockMvc
        .perform(multipart("/uploads/images").file(file).with(jwt().jwt(jwt -> jwt.subject(CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").isString())
        .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("https://cdn.example.com/uploads/")))
        .andExpect(jsonPath("$.data.contentType").value(MediaType.IMAGE_PNG_VALUE))
        .andExpect(jsonPath("$.data.sizeBytes").value(file.getSize()));

    assertThat(storageService.callCount).isEqualTo(1);
    assertThat(storageService.objectKey).matches("uploads/\\d{4}/\\d{2}/\\d+/[0-9a-f-]{36}\\.png");
    assertThat(storageService.bytes).isEqualTo(file.getBytes());
    assertThat(storageService.contentType).isEqualTo(MediaType.IMAGE_PNG_VALUE);

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_objects WHERE bucket = ? AND object_key = ? AND content_type = ? AND size_bytes = ?",
            Integer.class,
            "test-bucket",
            storageService.objectKey,
            MediaType.IMAGE_PNG_VALUE,
            file.getSize());
    assertThat(count).isEqualTo(1);
  }

  @Test
  void unauthenticatedUploadIsRejected() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "png-bytes".getBytes());

    mockMvc.perform(multipart("/uploads/images").file(file)).andExpect(status().isUnauthorized());

    assertThat(storageService.callCount).isZero();
  }

  @Test
  void unsupportedContentTypeIsRejected() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

    mockMvc
        .perform(multipart("/uploads/images").file(file).with(jwt().jwt(jwt -> jwt.subject(CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    assertThat(storageService.callCount).isZero();
  }

  @Test
  void missingContentTypeIsRejectedAsBadRequest() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "image", null, "bytes".getBytes());

    mockMvc
        .perform(multipart("/uploads/images").file(file).with(jwt().jwt(jwt -> jwt.subject(CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    assertThat(storageService.callCount).isZero();
  }

  @Test
  void oversizedFileIsRejected() throws Exception {
    byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
    MockMultipartFile file =
        new MockMultipartFile("file", "large.webp", "image/webp", oversized);

    mockMvc
        .perform(multipart("/uploads/images").file(file).with(jwt().jwt(jwt -> jwt.subject(CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    assertThat(storageService.callCount).isZero();
  }

  @TestConfiguration
  static class UploadTestConfig {
    @Bean
    @Primary
    FakeStorageService fakeStorageService() {
      return new FakeStorageService();
    }
  }

  static class FakeStorageService implements StorageService {
    private int callCount;
    private String objectKey;
    private byte[] bytes;
    private String contentType;

    @Override
    public String upload(String objectKey, byte[] bytes, String contentType) {
      this.callCount++;
      this.objectKey = objectKey;
      this.bytes = Arrays.copyOf(bytes, bytes.length);
      this.contentType = contentType;
      return "https://cdn.example.com/" + objectKey;
    }

    private void reset() {
      callCount = 0;
      objectKey = null;
      bytes = null;
      contentType = null;
    }
  }
}
