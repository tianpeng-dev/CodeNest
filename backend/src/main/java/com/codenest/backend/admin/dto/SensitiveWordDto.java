package com.codenest.backend.admin.dto;

import com.codenest.backend.moderation.SensitiveWordEntity;
import java.time.LocalDateTime;

public record SensitiveWordDto(
    String id, String word, String level, LocalDateTime createdAt, int hitCount) {
  public static SensitiveWordDto from(SensitiveWordEntity entity) {
    return new SensitiveWordDto(
        String.valueOf(entity.getId()),
        entity.getWord(),
        entity.getLevel(),
        entity.getCreatedAt(),
        entity.getHitCount() == null ? 0 : entity.getHitCount());
  }
}
