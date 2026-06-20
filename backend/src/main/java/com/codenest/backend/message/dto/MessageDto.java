package com.codenest.backend.message.dto;

import com.codenest.backend.message.MessageEntity;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;

public record MessageDto(
    String id,
    String threadId,
    UserDto sender,
    String content,
    LocalDateTime readAt,
    LocalDateTime createdAt) {
  public static MessageDto from(MessageEntity message, Long viewerId, UserEntity sender) {
    return new MessageDto(
        String.valueOf(message.getId()),
        String.valueOf(otherParticipantId(message, viewerId)),
        UserDto.from(sender),
        message.getContent(),
        message.getReadAt(),
        message.getCreatedAt());
  }

  private static Long otherParticipantId(MessageEntity message, Long viewerId) {
    return message.getSenderId().equals(viewerId) ? message.getReceiverId() : message.getSenderId();
  }
}
