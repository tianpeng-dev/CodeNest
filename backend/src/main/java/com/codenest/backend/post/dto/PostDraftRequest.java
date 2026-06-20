package com.codenest.backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostDraftRequest(
    @NotBlank @Size(max = 160) String title,
    @Size(max = 500) String summary,
    @NotBlank String content,
    String coverUrl,
    @NotNull Long categoryId,
    List<String> tags,
    String status) {}
