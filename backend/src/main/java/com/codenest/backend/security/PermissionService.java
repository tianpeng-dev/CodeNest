package com.codenest.backend.security;

import org.springframework.stereotype.Service;

@Service
public class PermissionService {
  public boolean isAdmin(CurrentUser user) {
    return user != null && "admin".equals(user.role());
  }

  public boolean isModerator(CurrentUser user) {
    return user != null && "moderator".equals(user.role());
  }

  public boolean isCategoryModerator(CurrentUser user, Long categoryId) {
    return false;
  }
}
