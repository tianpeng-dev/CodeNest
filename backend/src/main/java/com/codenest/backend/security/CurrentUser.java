package com.codenest.backend.security;

import com.codenest.backend.user.UserEntity;

public record CurrentUser(Long id, String clerkUserId, String username, String role, String status) {
  public static CurrentUser from(UserEntity user) {
    return new CurrentUser(
        user.getId(), user.getClerkUserId(), user.getUsername(), user.getRole(), user.getStatus());
  }
}
