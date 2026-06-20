package com.codenest.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminUserStatusRequest(String status, LocalDateTime muteUntil, Boolean muted) {}
