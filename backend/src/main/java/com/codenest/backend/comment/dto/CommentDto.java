package com.codenest.backend.comment.dto;

import com.codenest.backend.comment.CommentEntity;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;

public record CommentDto(
    String id, String postId, UserDto author, String content, LocalDateTime createdAt) {
  public static CommentDto from(CommentEntity comment, UserEntity author) {
    return new CommentDto(
        String.valueOf(comment.getId()),
        String.valueOf(comment.getPostId()),
        UserDto.from(author),
        comment.getContent(),
        comment.getCreatedAt());
  }
}
