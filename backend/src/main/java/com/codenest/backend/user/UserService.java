package com.codenest.backend.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.security.ClerkUserSyncService.ClerkProfile;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService extends ServiceImpl<UserMapper, UserEntity> {
  private static final Pattern INVALID_USERNAME_CHARS = Pattern.compile("[^a-zA-Z0-9_]");
  private static final int USERNAME_MAX_LENGTH = 64;

  @Transactional
  public UserEntity syncFromClerk(ClerkProfile profile) {
    UserEntity user = findByClerkUserId(profile.clerkUserId());
    if (user == null) {
      user = createUser(profile);
      save(user);
      return user;
    }

    boolean changed = applyProfileUpdates(user, profile);
    if (changed) {
      user.setUpdatedAt(LocalDateTime.now());
      updateById(user);
    }
    return user;
  }

  public UserEntity findByClerkUserId(String clerkUserId) {
    return getOne(
        new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getClerkUserId, clerkUserId), false);
  }

  private UserEntity createUser(ClerkProfile profile) {
    LocalDateTime now = LocalDateTime.now();
    UserEntity user = new UserEntity();
    user.setClerkUserId(profile.clerkUserId());
    user.setUsername(resolveUniqueUsername(profile.username(), profile.clerkUserId()));
    user.setDisplayName(defaultDisplayName(profile.displayName(), user.getUsername()));
    user.setAvatarUrl(defaultString(profile.avatarUrl()));
    user.setBio("");
    user.setRole("user");
    user.setStatus("active");
    user.setPostCount(0);
    user.setLikeCount(0);
    user.setFavoriteCount(0);
    user.setFollowerCount(0);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    return user;
  }

  private boolean applyProfileUpdates(UserEntity user, ClerkProfile profile) {
    boolean changed = false;

    String displayName = profile.displayName();
    if (displayName != null && !displayName.equals(user.getDisplayName())) {
      user.setDisplayName(displayName);
      changed = true;
    }

    String avatarUrl = profile.avatarUrl();
    if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
      user.setAvatarUrl(avatarUrl);
      changed = true;
    }

    return changed;
  }

  private String resolveUniqueUsername(String claimUsername, String clerkUserId) {
    String base = normalizeUsername(claimUsername);
    if (base == null) {
      base = normalizeUsername(clerkUserId);
    }
    if (base == null) {
      base = "user";
    }

    String candidate = truncate(base, USERNAME_MAX_LENGTH);
    if (!usernameExists(candidate)) {
      return candidate;
    }

    String suffix = "_" + Integer.toUnsignedString(clerkUserId.hashCode(), 36);
    return truncate(base, USERNAME_MAX_LENGTH - suffix.length()) + suffix;
  }

  private boolean usernameExists(String username) {
    return count(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)) > 0;
  }

  private String normalizeUsername(String value) {
    if (value == null) {
      return null;
    }

    String normalized =
        INVALID_USERNAME_CHARS.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
    normalized = normalized.replaceAll("_+", "_").replaceAll("^_|_$", "");
    return normalized.isEmpty() ? null : normalized;
  }

  private String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String defaultDisplayName(String displayName, String username) {
    return displayName == null ? username : displayName;
  }

  private String defaultString(String value) {
    return value == null ? "" : value;
  }
}
