package com.codenest.backend.message;

import com.codenest.backend.common.ApiResponse;
import com.codenest.backend.message.dto.MessageDto;
import com.codenest.backend.message.dto.MessageThreadDto;
import com.codenest.backend.message.dto.SendMessageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/messages/threads")
  public ApiResponse<List<MessageThreadDto>> threads() {
    return ApiResponse.ok(messageService.listThreads());
  }

  @GetMapping("/messages/threads/{threadId}")
  public ApiResponse<List<MessageDto>> thread(@PathVariable Long threadId) {
    return ApiResponse.ok(messageService.readThread(threadId));
  }

  @PostMapping("/messages/threads/{threadId}")
  public ApiResponse<MessageDto> send(
      @PathVariable Long threadId, @Valid @RequestBody SendMessageRequest request) {
    return ApiResponse.ok(messageService.send(threadId, request));
  }
}
