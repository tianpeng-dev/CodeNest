package com.codenest.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record SensitiveWordRequest(@NotBlank String word, @NotBlank String level) {}
