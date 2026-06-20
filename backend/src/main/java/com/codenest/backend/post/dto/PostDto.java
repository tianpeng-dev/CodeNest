package com.codenest.backend.post.dto;

import com.codenest.backend.category.dto.CategoryDto;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;
import java.util.List;

public record PostDto(
    String id,
    String title,
    String summary,
    String content,
    String coverUrl,
    UserDto author,
    CategoryDto category,
    List<String> tags,
    String status,
    int viewCount,
    int likeCount,
    int favoriteCount,
    int commentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt) {}
