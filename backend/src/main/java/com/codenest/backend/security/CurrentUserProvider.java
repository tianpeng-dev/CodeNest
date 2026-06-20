package com.codenest.backend.security;

import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.user.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
  private final ClerkUserSyncService clerkUserSyncService;

  public CurrentUserProvider(ClerkUserSyncService clerkUserSyncService) {
    this.clerkUserSyncService = clerkUserSyncService;
  }

  public CurrentUser requireCurrentUser() {
    return CurrentUser.from(requireCurrentUserEntity());
  }

  public UserEntity requireCurrentUserEntity() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return sync(authentication);
  }

  public UserEntity sync(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthorized");
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof Jwt jwt)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthorized");
    }

    return clerkUserSyncService.sync(jwt);
  }
}
