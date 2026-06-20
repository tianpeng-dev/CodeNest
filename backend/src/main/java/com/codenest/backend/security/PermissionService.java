package com.codenest.backend.security;

import org.springframework.stereotype.Service;

@Service
public class PermissionService {
  private final CategoryModeratorMapper categoryModeratorMapper;

  public PermissionService(CategoryModeratorMapper categoryModeratorMapper) {
    this.categoryModeratorMapper = categoryModeratorMapper;
  }

  public boolean isAdmin(CurrentUser user) {
    return user != null && "admin".equals(user.role());
  }

  public boolean isModerator(CurrentUser user) {
    return user != null && "moderator".equals(user.role());
  }

  public boolean isCategoryModerator(CurrentUser user, Long categoryId) {
    return isModerator(user)
        && categoryId != null
        && categoryModeratorMapper.existsAssignment(categoryId, user.id());
  }
}
