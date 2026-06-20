package com.codenest.backend.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codenest.backend.config.StorageProperties;
import com.codenest.backend.security.CurrentUser;
import com.codenest.backend.security.CurrentUserProvider;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class UploadControllerCleanupTest {
  @Test
  void deletesUploadedObjectWhenMetadataInsertFails() {
    StorageProperties properties = new StorageProperties();
    properties.setBucket("test-bucket");
    RecordingStorageService storageService = new RecordingStorageService();
    UploadController controller =
        new UploadController(
            new FixedCurrentUserProvider(),
            properties,
            storageService,
            failingFileObjectMapper());

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, validPngBytes());

    assertThatThrownBy(() -> controller.uploadImage(file))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("insert failed");

    assertThat(storageService.uploadedKey).isNotBlank();
    assertThat(storageService.deletedKey).isEqualTo(storageService.uploadedKey);
  }

  private FileObjectMapper failingFileObjectMapper() {
    return (FileObjectMapper)
        Proxy.newProxyInstance(
            FileObjectMapper.class.getClassLoader(),
            new Class<?>[] {FileObjectMapper.class},
            (proxy, method, args) -> {
              if ("insert".equals(method.getName())) {
                throw new RuntimeException("insert failed");
              }
              if ("toString".equals(method.getName())) {
                return "FailingFileObjectMapper";
              }
              return null;
            });
  }

  private byte[] validPngBytes() {
    return new byte[] {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };
  }

  private static class FixedCurrentUserProvider extends CurrentUserProvider {
    FixedCurrentUserProvider() {
      super(null);
    }

    @Override
    public CurrentUser requireCurrentUser() {
      return new CurrentUser(123L, "clerk_cleanup", "cleanup", "user", "active");
    }
  }

  private static class RecordingStorageService implements StorageService {
    private String uploadedKey;
    private String deletedKey;

    @Override
    public String upload(String objectKey, byte[] bytes, String contentType) {
      uploadedKey = objectKey;
      return "https://cdn.example.com/" + objectKey;
    }

    @Override
    public void delete(String objectKey) {
      deletedKey = objectKey;
    }
  }
}
