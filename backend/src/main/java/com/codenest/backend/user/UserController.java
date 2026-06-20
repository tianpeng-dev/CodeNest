package com.codenest.backend.user;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.social.FollowService;
import com.codenest.backend.user.dto.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
  private final CurrentUserProvider currentUserProvider;
  private final FollowService followService;
  private final UserService userService;

  public UserController(
      CurrentUserProvider currentUserProvider, FollowService followService, UserService userService) {
    this.currentUserProvider = currentUserProvider;
    this.followService = followService;
    this.userService = userService;
  }

  @GetMapping("/auth/me")
  public ApiResponse<UserDto> me() {
    return ApiResponse.ok(UserDto.from(currentUserProvider.requireCurrentUserEntity()));
  }

  @PostMapping("/auth/sync")
  public ApiResponse<UserDto> sync(Authentication authentication) {
    return ApiResponse.ok(UserDto.from(currentUserProvider.sync(authentication)));
  }

  @GetMapping("/users/{id}")
  public ApiResponse<UserDto> get(@PathVariable Long id) {
    return ApiResponse.ok(userService.getPublicProfile(id));
  }

  @PostMapping("/users/{id}/follow")
  public ApiResponse<UserDto> follow(@PathVariable Long id) {
    return ApiResponse.ok(followService.toggleFollow(id));
  }
}
