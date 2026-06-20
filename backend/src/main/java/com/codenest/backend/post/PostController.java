package com.codenest.backend.post;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.common.PageResult;
import com.codenest.backend.post.dto.CreatorAnalyticsDto;
import com.codenest.backend.post.dto.CreatorCommentDto;
import com.codenest.backend.post.dto.PostDraftRequest;
import com.codenest.backend.post.dto.PostDto;
import com.codenest.backend.post.dto.PostQuery;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {
  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @GetMapping("/posts")
  public ApiResponse<PageResult<PostDto>> listPublic(@ModelAttribute PostQuery query) {
    return ApiResponse.ok(postService.listPublic(query));
  }

  @GetMapping("/posts/{id}")
  public ApiResponse<PostDto> get(@PathVariable Long id) {
    return ApiResponse.ok(postService.getPublic(id));
  }

  @GetMapping("/creator/posts")
  public ApiResponse<PageResult<PostDto>> listCreator(@ModelAttribute PostQuery query) {
    return ApiResponse.ok(postService.listCreator(query));
  }

  @GetMapping("/creator/analytics")
  public ApiResponse<CreatorAnalyticsDto> creatorAnalytics() {
    return ApiResponse.ok(postService.creatorAnalytics());
  }

  @GetMapping("/creator/comments")
  public ApiResponse<List<CreatorCommentDto>> creatorComments() {
    return ApiResponse.ok(postService.creatorComments());
  }

  @PostMapping("/posts/drafts")
  public ApiResponse<PostDto> create(@Valid @RequestBody PostDraftRequest request) {
    return ApiResponse.ok(postService.create(request));
  }

  @PutMapping("/posts/{id}")
  public ApiResponse<PostDto> update(
      @PathVariable Long id, @Valid @RequestBody PostDraftRequest request) {
    return ApiResponse.ok(postService.update(id, request));
  }

  @PostMapping("/posts/{id}/publish")
  public ApiResponse<PostDto> publish(@PathVariable Long id) {
    return ApiResponse.ok(postService.publish(id));
  }

  @DeleteMapping("/posts/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    postService.softDelete(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/posts/{id}/like")
  public ApiResponse<PostDto> like(@PathVariable Long id) {
    return ApiResponse.ok(postService.toggleLike(id));
  }

  @PostMapping("/posts/{id}/dislike")
  public ApiResponse<PostDto> dislike(@PathVariable Long id) {
    return ApiResponse.ok(postService.toggleDislike(id));
  }

  @PostMapping("/posts/{id}/favorite")
  public ApiResponse<PostDto> favorite(@PathVariable Long id) {
    return ApiResponse.ok(postService.toggleFavorite(id));
  }
}
