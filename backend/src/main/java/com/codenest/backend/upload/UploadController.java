package com.codenest.backend.upload;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.config.StorageProperties;
import com.codenest.backend.security.CurrentUser;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.upload.dto.UploadResultDto;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {
  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
  private static final Map<String, String> EXTENSIONS =
      Map.of(
          MediaType.IMAGE_PNG_VALUE, "png",
          MediaType.IMAGE_JPEG_VALUE, "jpg",
          "image/webp", "webp",
          MediaType.IMAGE_GIF_VALUE, "gif");

  private final CurrentUserProvider currentUserProvider;
  private final StorageProperties storageProperties;
  private final StorageService storageService;
  private final FileObjectMapper fileObjectMapper;

  public UploadController(
      CurrentUserProvider currentUserProvider,
      StorageProperties storageProperties,
      StorageService storageService,
      FileObjectMapper fileObjectMapper) {
    this.currentUserProvider = currentUserProvider;
    this.storageProperties = storageProperties;
    this.storageService = storageService;
    this.fileObjectMapper = fileObjectMapper;
  }

  @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadResultDto> uploadImage(@RequestPart("file") MultipartFile file) {
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    String contentType = validateImage(file);
    byte[] bytes = bytes(file);
    validateImageSignature(contentType, bytes);
    String objectKey = objectKey(currentUser.id(), EXTENSIONS.get(contentType));
    String url = storageService.upload(objectKey, bytes, contentType);

    FileObjectEntity entity = new FileObjectEntity();
    entity.setOwnerId(currentUser.id());
    entity.setBucket(storageProperties.getBucket());
    entity.setObjectKey(objectKey);
    entity.setUrl(url);
    entity.setContentType(contentType);
    entity.setSizeBytes(file.getSize());
    entity.setCreatedAt(LocalDateTime.now());
    try {
      fileObjectMapper.insert(entity);
    } catch (RuntimeException exception) {
      deleteUploadedObject(objectKey, exception);
      throw exception;
    }

    return ApiResponse.ok(
        new UploadResultDto(String.valueOf(entity.getId()), url, contentType, file.getSize()));
  }

  private String validateImage(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }

    String contentType = file.getContentType();
    if (!StringUtils.hasText(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Missing content type");
    }

    if (!EXTENSIONS.containsKey(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported image type");
    }

    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File exceeds 5 MB");
    }

    return contentType;
  }

  private void validateImageSignature(String contentType, byte[] bytes) {
    boolean valid =
        switch (contentType) {
          case MediaType.IMAGE_PNG_VALUE -> hasPrefix(
              bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
          case MediaType.IMAGE_JPEG_VALUE -> hasPrefix(bytes, 0xFF, 0xD8, 0xFF);
          case MediaType.IMAGE_GIF_VALUE -> isGif(bytes);
          case "image/webp" -> isWebp(bytes);
          default -> false;
        };

    if (!valid) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid image content");
    }
  }

  private boolean hasPrefix(byte[] bytes, int... prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }

    for (int i = 0; i < prefix.length; i++) {
      if ((bytes[i] & 0xFF) != prefix[i]) {
        return false;
      }
    }

    return true;
  }

  private boolean isGif(byte[] bytes) {
    return hasPrefix(bytes, 'G', 'I', 'F', '8', '7', 'a')
        || hasPrefix(bytes, 'G', 'I', 'F', '8', '9', 'a');
  }

  private boolean isWebp(byte[] bytes) {
    return bytes.length >= 12
        && hasPrefix(bytes, 'R', 'I', 'F', 'F')
        && bytes[8] == 'W'
        && bytes[9] == 'E'
        && bytes[10] == 'B'
        && bytes[11] == 'P';
  }

  private void deleteUploadedObject(String objectKey, RuntimeException originalException) {
    try {
      storageService.delete(objectKey);
    } catch (RuntimeException deleteException) {
      originalException.addSuppressed(deleteException);
    }
  }

  private byte[] bytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unable to read uploaded file");
    }
  }

  private String objectKey(Long ownerId, String extension) {
    LocalDate now = LocalDate.now();
    return "uploads/"
        + YEAR.format(now)
        + "/"
        + MONTH.format(now)
        + "/"
        + ownerId
        + "/"
        + UUID.randomUUID()
        + "."
        + extension;
  }
}
