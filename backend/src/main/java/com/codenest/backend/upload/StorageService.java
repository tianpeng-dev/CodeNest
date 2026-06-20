package com.codenest.backend.upload;

public interface StorageService {
  String upload(String objectKey, byte[] bytes, String contentType);
}
