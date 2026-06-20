package com.codenest.backend.post.dto;

public record PostQuery(
    String keyword,
    Long categoryId,
    String categorySlug,
    Long authorId,
    String tags,
    String status,
    Integer page,
    Integer pageSize,
    String sortBy) {
  public int normalizedPage() {
    return page == null || page < 1 ? 1 : page;
  }

  public int normalizedPageSize() {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }
}
