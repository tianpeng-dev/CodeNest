package com.codenest.backend.message.dto;

import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;

public record MessageThreadDto(
    String id, UserDto participant, MessageDto lastMessage, long unreadCount, LocalDateTime updatedAt) {}
