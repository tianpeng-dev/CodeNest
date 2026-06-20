package com.codenest.backend.admin;

import com.codenest.backend.admin.dto.AdminAnalyticsDto;
import com.codenest.backend.admin.dto.AdminMetricDto;
import com.codenest.backend.admin.dto.AdminModeratorRequest;
import com.codenest.backend.admin.dto.AdminModeratorSectionDto;
import com.codenest.backend.admin.dto.AdminPostStatusRequest;
import com.codenest.backend.admin.dto.AdminUserStatusRequest;
import com.codenest.backend.admin.dto.SensitiveWordDto;
import com.codenest.backend.admin.dto.SensitiveWordRequest;
import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.post.dto.PostDto;
import com.codenest.backend.user.dto.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/admin/metrics")
  public ApiResponse<List<AdminMetricDto>> metrics() {
    return ApiResponse.ok(adminService.metrics());
  }

  @GetMapping("/admin/users")
  public ApiResponse<List<UserDto>> users() {
    return ApiResponse.ok(adminService.users());
  }

  @PatchMapping("/admin/users/{id}/status")
  public ApiResponse<UserDto> updateUserStatus(
      @PathVariable Long id, @RequestBody AdminUserStatusRequest request) {
    return ApiResponse.ok(adminService.updateUserStatus(id, request));
  }

  @GetMapping("/admin/posts")
  public ApiResponse<List<PostDto>> posts() {
    return ApiResponse.ok(adminService.posts());
  }

  @PatchMapping("/admin/posts/{id}/status")
  public ApiResponse<PostDto> updatePostStatus(
      @PathVariable Long id, @RequestBody AdminPostStatusRequest request) {
    return ApiResponse.ok(adminService.updatePostStatus(id, request));
  }

  @GetMapping("/admin/moderators")
  public ApiResponse<List<AdminModeratorSectionDto>> moderators() {
    return ApiResponse.ok(adminService.moderators());
  }

  @PostMapping("/admin/moderators")
  public ApiResponse<List<AdminModeratorSectionDto>> assignModerator(
      @Valid @RequestBody AdminModeratorRequest request) {
    return ApiResponse.ok(adminService.assignModerator(request));
  }

  @DeleteMapping("/admin/moderators/{id}")
  public ApiResponse<List<AdminModeratorSectionDto>> removeModerator(@PathVariable Long id) {
    return ApiResponse.ok(adminService.removeModerator(id));
  }

  @GetMapping("/admin/sensitive-words")
  public ApiResponse<List<SensitiveWordDto>> sensitiveWords() {
    return ApiResponse.ok(adminService.sensitiveWords());
  }

  @PostMapping("/admin/sensitive-words")
  public ApiResponse<SensitiveWordDto> createSensitiveWord(
      @Valid @RequestBody SensitiveWordRequest request) {
    return ApiResponse.ok(adminService.createSensitiveWord(request));
  }

  @PutMapping("/admin/sensitive-words/{id}")
  public ApiResponse<SensitiveWordDto> updateSensitiveWord(
      @PathVariable Long id, @Valid @RequestBody SensitiveWordRequest request) {
    return ApiResponse.ok(adminService.updateSensitiveWord(id, request));
  }

  @DeleteMapping("/admin/sensitive-words/{id}")
  public ApiResponse<Void> deleteSensitiveWord(@PathVariable Long id) {
    adminService.deleteSensitiveWord(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/admin/analytics")
  public ApiResponse<AdminAnalyticsDto> analytics() {
    return ApiResponse.ok(adminService.analytics());
  }
}
