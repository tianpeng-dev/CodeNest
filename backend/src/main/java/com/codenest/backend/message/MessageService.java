package com.codenest.backend.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.message.dto.MessageDto;
import com.codenest.backend.message.dto.MessageThreadDto;
import com.codenest.backend.message.dto.SendMessageRequest;
import com.codenest.backend.moderation.SensitiveWordService;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserMapper;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService extends ServiceImpl<MessageMapper, MessageEntity> {
  private final CurrentUserProvider currentUserProvider;
  private final UserMapper userMapper;
  private final SensitiveWordService sensitiveWordService;

  public MessageService(
      CurrentUserProvider currentUserProvider,
      UserMapper userMapper,
      SensitiveWordService sensitiveWordService) {
    this.currentUserProvider = currentUserProvider;
    this.userMapper = userMapper;
    this.sensitiveWordService = sensitiveWordService;
  }

  public List<MessageThreadDto> listThreads() {
    Long currentUserId = currentUserProvider.requireCurrentUser().id();
    List<MessageEntity> messages =
        list(
            new LambdaQueryWrapper<MessageEntity>()
                .and(
                    wrapper ->
                        wrapper
                            .eq(MessageEntity::getSenderId, currentUserId)
                            .or()
                            .eq(MessageEntity::getReceiverId, currentUserId))
                .orderByDesc(MessageEntity::getCreatedAt)
                .orderByDesc(MessageEntity::getId));

    Map<Long, ThreadAccumulator> threads = new LinkedHashMap<>();
    for (MessageEntity message : messages) {
      Long participantId = otherParticipantId(message, currentUserId);
      ThreadAccumulator accumulator =
          threads.computeIfAbsent(participantId, id -> new ThreadAccumulator(message));
      if (message.getReceiverId().equals(currentUserId) && message.getReadAt() == null) {
        accumulator.unreadCount++;
      }
    }

    List<MessageThreadDto> result = new ArrayList<>();
    for (Map.Entry<Long, ThreadAccumulator> entry : threads.entrySet()) {
      UserEntity participant = requireUser(entry.getKey());
      ThreadAccumulator accumulator = entry.getValue();
      result.add(
          new MessageThreadDto(
              String.valueOf(entry.getKey()),
              UserDto.from(participant),
              toDto(accumulator.lastMessage, currentUserId),
              accumulator.unreadCount,
              accumulator.lastMessage.getCreatedAt()));
    }
    return result;
  }

  @Transactional
  public List<MessageDto> readThread(Long participantId) {
    Long currentUserId = currentUserProvider.requireCurrentUser().id();
    requireParticipant(currentUserId, participantId);
    baseMapper.markIncomingRead(participantId, currentUserId);
    return listThreadMessages(currentUserId, participantId);
  }

  @Transactional
  public MessageDto send(Long participantId, SendMessageRequest request) {
    Long currentUserId = currentUserProvider.requireWritableCurrentUser().id();
    requireParticipant(currentUserId, participantId);
    String content = trimRequired(request.content(), "Message content is required");
    SensitiveWordService.ScanResult scanResult = sensitiveWordService.scan(content);
    sensitiveWordService.blockIfHigh(scanResult, "message", null, currentUserId);

    MessageEntity message = new MessageEntity();
    message.setSenderId(currentUserId);
    message.setReceiverId(participantId);
    message.setContent(content);
    message.setCreatedAt(LocalDateTime.now());
    save(message);
    sensitiveWordService.recordHits(scanResult, "message", message.getId(), currentUserId);
    return toDto(message, currentUserId);
  }

  private List<MessageDto> listThreadMessages(Long currentUserId, Long participantId) {
    return list(
            new LambdaQueryWrapper<MessageEntity>()
                .and(
                    wrapper ->
                        wrapper
                            .eq(MessageEntity::getSenderId, currentUserId)
                            .eq(MessageEntity::getReceiverId, participantId))
                .or(
                    wrapper ->
                        wrapper
                            .eq(MessageEntity::getSenderId, participantId)
                            .eq(MessageEntity::getReceiverId, currentUserId))
                .orderByAsc(MessageEntity::getCreatedAt)
                .orderByAsc(MessageEntity::getId))
        .stream()
        .map(message -> toDto(message, currentUserId))
        .toList();
  }

  private void requireParticipant(Long currentUserId, Long participantId) {
    if (currentUserId.equals(participantId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot message yourself");
    }
    requireUser(participantId);
  }

  private MessageDto toDto(MessageEntity message, Long viewerId) {
    UserEntity sender = requireUser(message.getSenderId());
    return MessageDto.from(message, viewerId, sender);
  }

  private UserEntity requireUser(Long id) {
    UserEntity user = userMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
    }
    return user;
  }

  private Long otherParticipantId(MessageEntity message, Long currentUserId) {
    return message.getSenderId().equals(currentUserId) ? message.getReceiverId() : message.getSenderId();
  }

  private String trimRequired(String value, String message) {
    if (value == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
    return trimmed;
  }

  private static final class ThreadAccumulator {
    private final MessageEntity lastMessage;
    private long unreadCount;

    private ThreadAccumulator(MessageEntity lastMessage) {
      this.lastMessage = lastMessage;
    }
  }
}
