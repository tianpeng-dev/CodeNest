package com.codenest.backend.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.message.dto.NotificationDto;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.user.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService extends ServiceImpl<NotificationMapper, NotificationEntity> {
  private static final String TYPE_COMMENT = "comment";
  private static final String TYPE_FOLLOW = "follow";

  private final CurrentUserProvider currentUserProvider;

  public NotificationService(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  public List<NotificationDto> listMine() {
    Long userId = currentUserProvider.requireCurrentUser().id();
    return list(
            new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getUserId, userId)
                .orderByDesc(NotificationEntity::getCreatedAt)
                .orderByDesc(NotificationEntity::getId))
        .stream()
        .map(NotificationDto::from)
        .toList();
  }

  @Transactional
  public NotificationDto markRead(Long id) {
    Long userId = currentUserProvider.requireCurrentUser().id();
    NotificationEntity notification = getById(id);
    if (notification == null || !userId.equals(notification.getUserId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Notification not found");
    }
    if (notification.getReadAt() == null) {
      notification.setReadAt(LocalDateTime.now());
      updateById(notification);
    }
    return NotificationDto.from(notification);
  }

  public void createFollowNotification(UserEntity target, UserEntity follower) {
    if (target.getId().equals(follower.getId())) {
      return;
    }
    create(
        target.getId(),
        TYPE_FOLLOW,
        "New follower",
        follower.getDisplayName() + " started following you");
  }

  public void createCommentNotification(UserEntity postAuthor, UserEntity commenter, String postTitle) {
    if (postAuthor.getId().equals(commenter.getId())) {
      return;
    }
    create(
        postAuthor.getId(),
        TYPE_COMMENT,
        "New comment",
        commenter.getDisplayName() + " commented on your post: " + postTitle);
  }

  private void create(Long userId, String type, String title, String content) {
    NotificationEntity notification = new NotificationEntity();
    notification.setUserId(userId);
    notification.setType(type);
    notification.setTitle(title);
    notification.setContent(content);
    notification.setCreatedAt(LocalDateTime.now());
    save(notification);
  }
}
