package com.codenest.backend.security;

import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserService;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class ClerkUserSyncService {
  private final UserService userService;

  public ClerkUserSyncService(UserService userService) {
    this.userService = userService;
  }

  public UserEntity sync(Jwt jwt) {
    String clerkUserId = trimToNull(jwt.getSubject());
    if (clerkUserId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthorized");
    }

    Map<String, Object> claims = jwt.getClaims();
    ClerkProfile profile =
        new ClerkProfile(
            clerkUserId,
            firstClaim(claims, "username", "preferred_username"),
            displayName(claims),
            firstClaim(claims, "image_url", "picture"));

    return userService.syncFromClerk(profile);
  }

  private String displayName(Map<String, Object> claims) {
    String name = firstClaim(claims, "name");
    if (name != null) {
      return name;
    }

    String givenName = firstClaim(claims, "given_name");
    String familyName = firstClaim(claims, "family_name");
    String combined = joinNames(givenName, familyName);
    if (combined != null) {
      return combined;
    }

    return firstClaim(claims, "username", "preferred_username");
  }

  private String firstClaim(Map<String, Object> claims, String... keys) {
    for (String key : keys) {
      Object value = claims.get(key);
      if (value instanceof String text) {
        String trimmed = trimToNull(text);
        if (trimmed != null) {
          return trimmed;
        }
      }
    }

    return null;
  }

  private String joinNames(String givenName, String familyName) {
    if (givenName == null) {
      return familyName;
    }
    if (familyName == null) {
      return givenName;
    }
    return givenName + " " + familyName;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record ClerkProfile(
      String clerkUserId, String username, String displayName, String avatarUrl) {}
}
