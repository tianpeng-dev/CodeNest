package com.codenest.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @NotBlank @Size(max = 80) String name,
    @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
    @Size(max = 500) String description,
    @Size(max = 512) String coverUrl,
    Integer sortOrder,
    @Pattern(regexp = "^(active|disabled)$") String status) {}
