package com.codenest.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminModeratorRequest(@NotNull Long categoryId, @NotNull Long userId) {}
