package com.codenest.backend.social;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.message.NotificationService;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserMapper;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService extends ServiceImpl<FollowMapper, FollowEntity> {
  private final CurrentUserProvider currentUserProvider;
  private final UserMapper userMapper;
  private final NotificationService notificationService;

  public FollowService(
      CurrentUserProvider currentUserProvider,
      UserMapper userMapper,
      NotificationService notificationService) {
    this.currentUserProvider = currentUserProvider;
    this.userMapper = userMapper;
    this.notificationService = notificationService;
  }

  @Transactional
  public UserDto toggleFollow(Long targetUserId) {
    UserEntity follower = currentUserProvider.requireWritableCurrentUserEntity();
    if (follower.getId().equals(targetUserId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot follow yourself");
    }
    UserEntity target = requireUser(targetUserId);
    FollowEntity existing = findFollow(follower.getId(), target.getId());
    if (existing == null) {
      createFollow(follower, target);
    } else if (removeById(existing.getId())) {
      userMapper.decrementFollowerCount(target.getId());
    }
    return UserDto.from(requireUser(target.getId()));
  }

  private void createFollow(UserEntity follower, UserEntity target) {
    FollowEntity follow = new FollowEntity();
    follow.setFollowerId(follower.getId());
    follow.setFollowingId(target.getId());
    follow.setCreatedAt(LocalDateTime.now());
    try {
      save(follow);
      userMapper.incrementFollowerCount(target.getId());
      notificationService.createFollowNotification(target, follower);
    } catch (DuplicateKeyException exception) {
      // Another request won the toggle race; return the current target state.
    }
  }

  private FollowEntity findFollow(Long followerId, Long followingId) {
    return getOne(
        new LambdaQueryWrapper<FollowEntity>()
            .eq(FollowEntity::getFollowerId, followerId)
            .eq(FollowEntity::getFollowingId, followingId),
        false);
  }

  private UserEntity requireUser(Long id) {
    UserEntity user = userMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
    }
    return user;
  }
}
