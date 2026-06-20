package com.codenest.backend.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.comment.dto.CommentDto;
import com.codenest.backend.comment.dto.CreateCommentRequest;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.moderation.SensitiveWordService;
import com.codenest.backend.post.PostEntity;
import com.codenest.backend.post.PostMapper;
import com.codenest.backend.security.CurrentUser;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.security.PermissionService;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService extends ServiceImpl<CommentMapper, CommentEntity> {
  private static final String POST_STATUS_PUBLISHED = "published";
  private static final String COMMENT_STATUS_VISIBLE = "visible";
  private static final String USER_STATUS_BANNED = "banned";

  private final PostMapper postMapper;
  private final UserMapper userMapper;
  private final CurrentUserProvider currentUserProvider;
  private final PermissionService permissionService;
  private final SensitiveWordService sensitiveWordService;

  public CommentService(
      PostMapper postMapper,
      UserMapper userMapper,
      CurrentUserProvider currentUserProvider,
      PermissionService permissionService,
      SensitiveWordService sensitiveWordService) {
    this.postMapper = postMapper;
    this.userMapper = userMapper;
    this.currentUserProvider = currentUserProvider;
    this.permissionService = permissionService;
    this.sensitiveWordService = sensitiveWordService;
  }

  public List<CommentDto> listPublic(Long postId) {
    requirePublishedPost(postId);
    return list(
            new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getPostId, postId)
                .eq(CommentEntity::getStatus, COMMENT_STATUS_VISIBLE)
                .orderByAsc(CommentEntity::getCreatedAt)
                .orderByAsc(CommentEntity::getId))
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public CommentDto create(Long postId, CreateCommentRequest request) {
    PostEntity post = requirePublishedPost(postId);
    UserEntity author = currentUserProvider.requireCurrentUserEntity();
    requireCanComment(author);

    String content = trimRequired(request.content(), "Comment content is required");
    SensitiveWordService.ScanResult scanResult = sensitiveWordService.scan(content);
    sensitiveWordService.blockIfHigh(scanResult, "comment", null, author.getId());

    LocalDateTime now = LocalDateTime.now();
    CommentEntity comment = new CommentEntity();
    comment.setPostId(post.getId());
    comment.setAuthorId(author.getId());
    comment.setContent(content);
    comment.setStatus(COMMENT_STATUS_VISIBLE);
    comment.setCreatedAt(now);
    comment.setUpdatedAt(now);
    save(comment);
    postMapper.incrementCommentCount(post.getId());
    sensitiveWordService.recordHits(scanResult, "comment", comment.getId(), author.getId());
    return toDto(comment);
  }

  @Transactional
  public void softDelete(Long id) {
    CommentEntity comment = requireComment(id);
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    PostEntity post = requirePost(comment.getPostId());
    if (!canDelete(comment, post, currentUser)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Forbidden");
    }
    if (baseMapper.markDeletedIfVisible(comment.getId()) == 1) {
      postMapper.decrementCommentCount(comment.getPostId());
    }
  }

  private boolean canDelete(CommentEntity comment, PostEntity post, CurrentUser currentUser) {
    return currentUser != null
        && (Objects.equals(comment.getAuthorId(), currentUser.id())
            || permissionService.isAdmin(currentUser)
            || permissionService.isCategoryModerator(currentUser, post.getCategoryId()));
  }

  private void requireCanComment(UserEntity user) {
    if (USER_STATUS_BANNED.equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Forbidden");
    }
    LocalDateTime muteUntil = user.getMuteUntil();
    if (muteUntil != null && muteUntil.isAfter(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Forbidden");
    }
  }

  private PostEntity requirePublishedPost(Long postId) {
    PostEntity post = requirePost(postId);
    if (!POST_STATUS_PUBLISHED.equals(post.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return post;
  }

  private PostEntity requirePost(Long postId) {
    PostEntity post = postMapper.selectById(postId);
    if (post == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return post;
  }

  private CommentEntity requireComment(Long id) {
    CommentEntity comment = getById(id);
    if (comment == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Comment not found");
    }
    return comment;
  }

  private CommentDto toDto(CommentEntity comment) {
    UserEntity author = userMapper.selectById(comment.getAuthorId());
    if (author == null) {
      throw new BusinessException(ErrorCode.SERVER_ERROR, "Comment relation is invalid");
    }
    return CommentDto.from(comment, author);
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
}
