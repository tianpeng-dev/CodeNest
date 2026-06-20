package com.codenest.backend.category.dto;

import com.codenest.backend.category.CategoryEntity;

public record CategoryDto(
    String id, String name, String slug, String description, int postCount) {
  public static CategoryDto from(CategoryEntity category) {
    return new CategoryDto(
        String.valueOf(category.getId()),
        category.getName(),
        category.getSlug(),
        category.getDescription(),
        defaultInt(category.getPostCount()));
  }

  private static int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }
}
