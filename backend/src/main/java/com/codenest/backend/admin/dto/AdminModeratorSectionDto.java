package com.codenest.backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminModeratorSectionDto(
    String id,
    String sectionName,
    String description,
    int moderatorCount,
    List<AdminModeratorDto> moderators,
    LocalDateTime updatedAt) {}
