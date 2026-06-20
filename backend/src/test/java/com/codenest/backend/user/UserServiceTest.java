package com.codenest.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.codenest.backend.security.ClerkUserSyncService.ClerkProfile;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
class UserServiceTest {
  private final UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);
  private final UserService userService = new UserService();

  UserServiceTest() {
    ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
  }

  @Test
  void syncFromClerkReturnsExistingUserWhenInsertRacesOnClerkId() {
    UserEntity existing = user("clerk_race_1", "racepeng", "Race Peng", "admin", "banned");
    when(userMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null, existing);
    when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(), List.of(existing));
    when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
    when(userMapper.insert(any(UserEntity.class)))
        .thenThrow(new DuplicateKeyException("uk_users_clerk_user_id"));

    UserEntity result =
        userService.syncFromClerk(
            new ClerkProfile("clerk_race_1", "racepeng", "Updated Name", "avatar.png"));

    assertThat(result).isSameAs(existing);
    assertThat(result.getRole()).isEqualTo("admin");
    assertThat(result.getStatus()).isEqualTo("banned");
    assertThat(result.getDisplayName()).isEqualTo("Updated Name");
    assertThat(result.getAvatarUrl()).isEqualTo("avatar.png");
    verify(userMapper).updateById(existing);
  }

  @Test
  void syncFromClerkRetriesWithNewSuffixWhenUsernameInsertRaces() {
    when(userMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null);
    when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
    when(userMapper.insert(any(UserEntity.class)))
        .thenThrow(new DuplicateKeyException("uk_users_username"))
        .thenReturn(1);

    UserEntity result =
        userService.syncFromClerk(new ClerkProfile("clerk_race_2", "same_name", null, null));

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userMapper, atLeast(2)).insert(captor.capture());
    List<UserEntity> insertedUsers = captor.getAllValues();

    assertThat(insertedUsers.get(0).getUsername()).isEqualTo("same_name");
    assertThat(insertedUsers.get(1).getUsername()).startsWith("same_name_");
    assertThat(result.getUsername()).isEqualTo(insertedUsers.get(1).getUsername());
  }

  private UserEntity user(
      String clerkUserId, String username, String displayName, String role, String status) {
    UserEntity user = new UserEntity();
    user.setId(42L);
    user.setClerkUserId(clerkUserId);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setAvatarUrl("");
    user.setBio("");
    user.setRole(role);
    user.setStatus(status);
    user.setPostCount(0);
    user.setLikeCount(0);
    user.setFavoriteCount(0);
    user.setFollowerCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    return user;
  }
}
