package com.codenest.backend.comment;

import com.codenest.backend.comment.dto.CommentDto;
import com.codenest.backend.comment.dto.CreateCommentRequest;
import com.codenest.backend.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/posts/{postId}/comments")
  public ApiResponse<List<CommentDto>> list(@PathVariable Long postId) {
    return ApiResponse.ok(commentService.listPublic(postId));
  }

  @PostMapping("/posts/{postId}/comments")
  public ApiResponse<CommentDto> create(
      @PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
    return ApiResponse.ok(commentService.create(postId, request));
  }

  @DeleteMapping("/comments/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    commentService.softDelete(id);
    return ApiResponse.ok(null);
  }
}
