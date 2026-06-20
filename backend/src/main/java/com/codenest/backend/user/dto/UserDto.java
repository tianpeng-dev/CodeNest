package com.codenest.backend.user.dto;

import com.codenest.backend.user.UserEntity;
import java.time.LocalDateTime;

public record UserDto(
    String id,
    String username,
    String displayName,
    String avatarUrl,
    String bio,
    String role,
    String status,
    LocalDateTime muteUntil,
    int postCount,
    int likeCount,
    int favoriteCount,
    int followerCount) {
  public static UserDto from(UserEntity user) {
    return new UserDto(
        String.valueOf(user.getId()),
        user.getUsername(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        user.getBio(),
        user.getRole(),
        user.getStatus(),
        user.getMuteUntil(),
        defaultInt(user.getPostCount()),
        defaultInt(user.getLikeCount()),
        defaultInt(user.getFavoriteCount()),
        defaultInt(user.getFollowerCount()));
  }

  private static int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }
}
