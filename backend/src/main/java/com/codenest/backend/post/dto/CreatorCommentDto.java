package com.codenest.backend.post.dto;

import com.codenest.backend.comment.CommentEntity;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;

public record CreatorCommentDto(
    String id,
    String postId,
    UserDto author,
    String content,
    LocalDateTime createdAt,
    PostSummaryDto post) {
  public static CreatorCommentDto from(
      CommentEntity comment, UserEntity author, PostSummaryDto post) {
    return new CreatorCommentDto(
        String.valueOf(comment.getId()),
        String.valueOf(comment.getPostId()),
        UserDto.from(author),
        comment.getContent(),
        comment.getCreatedAt(),
        post);
  }
}
