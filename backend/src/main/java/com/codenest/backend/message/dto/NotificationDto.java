package com.codenest.backend.message.dto;

import com.codenest.backend.message.NotificationEntity;
import java.time.LocalDateTime;

public record NotificationDto(
    String id, String title, String content, LocalDateTime readAt, LocalDateTime createdAt) {
  public static NotificationDto from(NotificationEntity notification) {
    return new NotificationDto(
        String.valueOf(notification.getId()),
        notification.getTitle(),
        notification.getContent(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }
}
