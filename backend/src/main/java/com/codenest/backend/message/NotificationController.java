package com.codenest.backend.message;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.message.dto.NotificationDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/notifications")
  public ApiResponse<List<NotificationDto>> list() {
    return ApiResponse.ok(notificationService.listMine());
  }

  @PostMapping("/notifications/{id}/read")
  public ApiResponse<NotificationDto> read(@PathVariable Long id) {
    return ApiResponse.ok(notificationService.markRead(id));
  }
}
