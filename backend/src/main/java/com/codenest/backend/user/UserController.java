package com.codenest.backend.user;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.user.dto.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class UserController {
  private final CurrentUserProvider currentUserProvider;

  public UserController(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/me")
  public ApiResponse<UserDto> me() {
    return ApiResponse.ok(UserDto.from(currentUserProvider.requireCurrentUserEntity()));
  }

  @PostMapping("/sync")
  public ApiResponse<UserDto> sync(Authentication authentication) {
    return ApiResponse.ok(UserDto.from(currentUserProvider.sync(authentication)));
  }
}
