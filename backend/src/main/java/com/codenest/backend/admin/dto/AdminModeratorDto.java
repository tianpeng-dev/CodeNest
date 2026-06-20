package com.codenest.backend.admin.dto;

public record AdminModeratorDto(
    String id, String username, String displayName, String status, String avatarUrl) {}
