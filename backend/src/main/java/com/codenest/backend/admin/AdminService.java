package com.codenest.backend.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenest.backend.admin.dto.AdminAnalyticsDto;
import com.codenest.backend.admin.dto.AdminCountGroupDto;
import com.codenest.backend.admin.dto.AdminMetricDto;
import com.codenest.backend.admin.dto.AdminModeratorDto;
import com.codenest.backend.admin.dto.AdminModeratorRequest;
import com.codenest.backend.admin.dto.AdminModeratorRow;
import com.codenest.backend.admin.dto.AdminModeratorSectionDto;
import com.codenest.backend.admin.dto.AdminPostStatusRequest;
import com.codenest.backend.admin.dto.AdminUserStatusRequest;
import com.codenest.backend.admin.dto.SensitiveWordDto;
import com.codenest.backend.admin.dto.SensitiveWordRequest;
import com.codenest.backend.category.CategoryEntity;
import com.codenest.backend.category.CategoryMapper;
import com.codenest.backend.category.dto.CategoryDto;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.common.PageResult;
import com.codenest.backend.message.NotificationService;
import com.codenest.backend.moderation.SensitiveWordEntity;
import com.codenest.backend.moderation.SensitiveWordHitEntity;
import com.codenest.backend.moderation.SensitiveWordHitMapper;
import com.codenest.backend.moderation.SensitiveWordMapper;
import com.codenest.backend.post.PostEntity;
import com.codenest.backend.post.PostMapper;
import com.codenest.backend.post.PostTagEntity;
import com.codenest.backend.post.PostTagMapper;
import com.codenest.backend.post.dto.PostDto;
import com.codenest.backend.post.dto.PostQuery;
import com.codenest.backend.security.CurrentUser;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserMapper;
import com.codenest.backend.user.dto.UserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private static final String ROLE_ADMIN = "admin";
  private static final String ROLE_MODERATOR = "moderator";
  private static final String ROLE_USER = "user";
  private static final String USER_STATUS_ACTIVE = "active";
  private static final String USER_STATUS_BANNED = "banned";
  private static final String POST_STATUS_PUBLISHED = "published";
  private static final String POST_STATUS_HIDDEN = "hidden";
  private static final String POST_STATUS_DELETED = "deleted";

  private final UserMapper userMapper;
  private final PostMapper postMapper;
  private final PostTagMapper postTagMapper;
  private final CategoryMapper categoryMapper;
  private final SensitiveWordMapper sensitiveWordMapper;
  private final SensitiveWordHitMapper sensitiveWordHitMapper;
  private final AuditLogMapper auditLogMapper;
  private final AdminCategoryModeratorMapper moderatorMapper;
  private final CurrentUserProvider currentUserProvider;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  public AdminService(
      UserMapper userMapper,
      PostMapper postMapper,
      PostTagMapper postTagMapper,
      CategoryMapper categoryMapper,
      SensitiveWordMapper sensitiveWordMapper,
      SensitiveWordHitMapper sensitiveWordHitMapper,
      AuditLogMapper auditLogMapper,
      AdminCategoryModeratorMapper moderatorMapper,
      CurrentUserProvider currentUserProvider,
      NotificationService notificationService,
      ObjectMapper objectMapper) {
    this.userMapper = userMapper;
    this.postMapper = postMapper;
    this.postTagMapper = postTagMapper;
    this.categoryMapper = categoryMapper;
    this.sensitiveWordMapper = sensitiveWordMapper;
    this.sensitiveWordHitMapper = sensitiveWordHitMapper;
    this.auditLogMapper = auditLogMapper;
    this.moderatorMapper = moderatorMapper;
    this.currentUserProvider = currentUserProvider;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  public List<AdminMetricDto> metrics() {
    return List.of(
        new AdminMetricDto("Total users", userMapper.selectCount(null), 0),
        new AdminMetricDto(
            "Published posts", countPostsByStatus(POST_STATUS_PUBLISHED), 0),
        new AdminMetricDto("Hidden posts", countPostsByStatus(POST_STATUS_HIDDEN), 0),
        new AdminMetricDto("Sensitive hits", sensitiveWordHitMapper.selectCount(null), 0));
  }

  public List<UserDto> users() {
    return userMapper
        .selectList(
            new LambdaQueryWrapper<UserEntity>()
                .orderByAsc(UserEntity::getId))
        .stream()
        .map(UserDto::from)
        .toList();
  }

  @Transactional
  public UserDto updateUserStatus(Long id, AdminUserStatusRequest request) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    UserEntity user = requireUser(id);
    String previousStatus = user.getStatus();
    LocalDateTime previousMuteUntil = user.getMuteUntil();

    String status = normalizeOptional(request.status());
    if (status != null) {
      if (!USER_STATUS_ACTIVE.equals(status) && !USER_STATUS_BANNED.equals(status)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported user status");
      }
      if (Objects.equals(operator.id(), user.getId()) && ROLE_ADMIN.equals(user.getRole())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Admins cannot change their own status");
      }
      user.setStatus(status);
    }

    if (Boolean.FALSE.equals(request.muted())) {
      user.setMuteUntil(null);
    } else if (request.muteUntil() != null) {
      user.setMuteUntil(request.muteUntil());
    } else if (Boolean.TRUE.equals(request.muted())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "muteUntil is required");
    }

    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
    audit(
        operator.id(),
        "user.status.update",
        "user",
        user.getId(),
        details(
            "previousStatus",
            previousStatus,
            "status",
            user.getStatus(),
            "previousMuteUntil",
            previousMuteUntil,
            "muteUntil",
            user.getMuteUntil()));
    return UserDto.from(user);
  }

  public PageResult<PostDto> posts(PostQuery postQuery) {
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    LambdaQueryWrapper<PostEntity> query =
        new LambdaQueryWrapper<PostEntity>()
            .orderByDesc(PostEntity::getUpdatedAt)
            .orderByDesc(PostEntity::getId);
    if (ROLE_MODERATOR.equals(currentUser.role())) {
      List<Long> categoryIds = moderatorMapper.selectCategoryIdsByUserId(currentUser.id());
      if (categoryIds.isEmpty()) {
        return new PageResult<>(
            List.of(), 0, postQuery.normalizedPage(), postQuery.normalizedPageSize());
      }
      query.in(PostEntity::getCategoryId, categoryIds);
    }
    if (postQuery.status() != null && !postQuery.status().isBlank()) {
      query.eq(PostEntity::getStatus, postQuery.status().trim());
    }
    if (postQuery.categoryId() != null) {
      query.eq(PostEntity::getCategoryId, postQuery.categoryId());
    }
    if (postQuery.authorId() != null) {
      query.eq(PostEntity::getAuthorId, postQuery.authorId());
    }
    if (postQuery.keyword() != null && !postQuery.keyword().isBlank()) {
      String keyword = postQuery.keyword().trim();
      query.and(
          nested ->
              nested
                  .like(PostEntity::getTitle, keyword)
                  .or()
                  .like(PostEntity::getSummary, keyword)
                  .or()
                  .like(PostEntity::getContent, keyword));
    }

    Page<PostEntity> page =
        new Page<>(postQuery.normalizedPage(), postQuery.normalizedPageSize());
    Page<PostEntity> result = postMapper.selectPage(page, query);
    return new PageResult<>(
        result.getRecords().stream().map(this::toPostDto).toList(),
        result.getTotal(),
        postQuery.normalizedPage(),
        postQuery.normalizedPageSize());
  }

  @Transactional
  public PostDto updatePostStatus(Long id, AdminPostStatusRequest request) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    PostEntity post = requirePost(id);
    requirePostScope(operator, post);

    String previousStatus = post.getStatus();
    String status = normalizeRequired(request.status(), "Status is required");
    LocalDateTime now = LocalDateTime.now();
    switch (status) {
      case POST_STATUS_HIDDEN -> {
        post.setStatus(POST_STATUS_HIDDEN);
        post.setHiddenReason(defaultReason(request.reason(), "Hidden by moderation"));
        notificationService.createPostHiddenNotification(requireUser(post.getAuthorId()), post.getTitle());
      }
      case POST_STATUS_PUBLISHED -> {
        post.setStatus(POST_STATUS_PUBLISHED);
        post.setHiddenReason(null);
        if (post.getPublishedAt() == null) {
          post.setPublishedAt(now);
        }
      }
      case POST_STATUS_DELETED -> {
        post.setStatus(POST_STATUS_DELETED);
        post.setHiddenReason(defaultReason(request.reason(), "Deleted by moderation"));
      }
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported post status");
    }
    post.setUpdatedAt(now);
    postMapper.updateById(post);
    audit(
        operator.id(),
        "post.status.update",
        "post",
        post.getId(),
        details(
            "previousStatus",
            previousStatus,
            "status",
            post.getStatus(),
            "reason",
            post.getHiddenReason()));
    return toPostDto(post);
  }

  public List<AdminModeratorSectionDto> moderators() {
    Map<Long, List<AdminModeratorRow>> rowsByCategory =
        moderatorMapper.selectModeratorRows().stream()
            .collect(Collectors.groupingBy(AdminModeratorRow::getCategoryId, LinkedHashMap::new, Collectors.toList()));
    List<AdminModeratorSectionDto> sections = new ArrayList<>();
    List<CategoryEntity> categories =
        categoryMapper.selectList(
            new LambdaQueryWrapper<CategoryEntity>()
                .orderByAsc(CategoryEntity::getSortOrder)
                .orderByAsc(CategoryEntity::getId));
    for (CategoryEntity category : categories) {
      List<AdminModeratorRow> rows = rowsByCategory.getOrDefault(category.getId(), List.of());
      List<AdminModeratorDto> moderators =
          rows.stream()
              .map(
                  row ->
                      new AdminModeratorDto(
                          String.valueOf(row.getId()),
                          row.getUsername(),
                          row.getDisplayName(),
                          row.getStatus(),
                          row.getAvatarUrl()))
              .toList();
      sections.add(
          new AdminModeratorSectionDto(
              String.valueOf(category.getId()),
              category.getName(),
              category.getDescription(),
              moderators.size(),
              moderators,
              category.getUpdatedAt()));
    }
    return sections;
  }

  @Transactional
  public List<AdminModeratorSectionDto> assignModerator(AdminModeratorRequest request) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    requireCategory(request.categoryId());
    UserEntity user = requireUser(request.userId());
    if (moderatorMapper.countByCategoryAndUser(request.categoryId(), request.userId()) > 0) {
      throw new BusinessException(ErrorCode.DUPLICATE, "Moderator assignment already exists");
    }
    try {
      moderatorMapper.insertAssignment(request.categoryId(), request.userId(), operator.id());
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(ErrorCode.DUPLICATE, "Moderator assignment already exists");
    }
    if (ROLE_USER.equals(user.getRole())) {
      user.setRole(ROLE_MODERATOR);
      user.setUpdatedAt(LocalDateTime.now());
      userMapper.updateById(user);
    }
    audit(
        operator.id(),
        "moderator.assign",
        "category_moderator",
        request.categoryId(),
        details("categoryId", request.categoryId(), "userId", request.userId()));
    return moderators();
  }

  @Transactional
  public List<AdminModeratorSectionDto> removeModerator(Long assignmentId) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    Long userId = moderatorMapper.selectUserIdById(assignmentId);
    if (userId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Moderator assignment not found");
    }
    moderatorMapper.deleteAssignment(assignmentId);
    UserEntity user = requireUser(userId);
    if (ROLE_MODERATOR.equals(user.getRole()) && moderatorMapper.countByUserId(userId) == 0) {
      user.setRole(ROLE_USER);
      user.setUpdatedAt(LocalDateTime.now());
      userMapper.updateById(user);
    }
    audit(
        operator.id(),
        "moderator.remove",
        "category_moderator",
        assignmentId,
        details("assignmentId", assignmentId, "userId", userId));
    return moderators();
  }

  public List<SensitiveWordDto> sensitiveWords() {
    return sensitiveWordMapper
        .selectList(
            new LambdaQueryWrapper<SensitiveWordEntity>()
                .orderByDesc(SensitiveWordEntity::getCreatedAt)
                .orderByDesc(SensitiveWordEntity::getId))
        .stream()
        .map(SensitiveWordDto::from)
        .toList();
  }

  @Transactional
  public SensitiveWordDto createSensitiveWord(SensitiveWordRequest request) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    SensitiveWordEntity entity = new SensitiveWordEntity();
    entity.setWord(normalizeWord(request.word()));
    entity.setLevel(normalizeLevel(request.level()));
    entity.setHitCount(0);
    entity.setCreatedBy(operator.id());
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(entity.getCreatedAt());
    ensureSensitiveWordAvailable(entity.getWord(), null);
    try {
      sensitiveWordMapper.insert(entity);
    } catch (DuplicateKeyException exception) {
      throw duplicateSensitiveWord();
    }
    audit(
        operator.id(),
        "sensitive_word.create",
        "sensitive_word",
        entity.getId(),
        details("word", entity.getWord(), "level", entity.getLevel()));
    return SensitiveWordDto.from(entity);
  }

  @Transactional
  public SensitiveWordDto updateSensitiveWord(Long id, SensitiveWordRequest request) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    SensitiveWordEntity entity = requireSensitiveWord(id);
    String previousWord = entity.getWord();
    String previousLevel = entity.getLevel();
    entity.setWord(normalizeWord(request.word()));
    entity.setLevel(normalizeLevel(request.level()));
    entity.setUpdatedAt(LocalDateTime.now());
    ensureSensitiveWordAvailable(entity.getWord(), id);
    try {
      sensitiveWordMapper.updateById(entity);
    } catch (DuplicateKeyException exception) {
      throw duplicateSensitiveWord();
    }
    audit(
        operator.id(),
        "sensitive_word.update",
        "sensitive_word",
        entity.getId(),
        details(
            "previousWord",
            previousWord,
            "word",
            entity.getWord(),
            "previousLevel",
            previousLevel,
            "level",
            entity.getLevel()));
    return SensitiveWordDto.from(entity);
  }

  @Transactional
  public void deleteSensitiveWord(Long id) {
    CurrentUser operator = currentUserProvider.requireCurrentUser();
    SensitiveWordEntity entity = requireSensitiveWord(id);
    sensitiveWordHitMapper.delete(
        new LambdaQueryWrapper<SensitiveWordHitEntity>()
            .eq(SensitiveWordHitEntity::getWordId, id));
    sensitiveWordMapper.deleteById(id);
    audit(
        operator.id(),
        "sensitive_word.delete",
        "sensitive_word",
        id,
        details("word", entity.getWord(), "level", entity.getLevel()));
  }

  public AdminAnalyticsDto analytics() {
    return new AdminAnalyticsDto(
        metrics(),
        countPostsByStatus(),
        countUsersByStatus(),
        countSensitiveHitsByLevel());
  }

  public void audit(
      Long operatorId,
      String action,
      String resourceType,
      Long resourceId,
      Map<String, ?> details) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setOperatorId(operatorId);
    entity.setAction(action);
    entity.setResourceType(resourceType);
    entity.setResourceId(resourceId);
    entity.setDetailJson(toJson(details));
    entity.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(entity);
  }

  public void auditCurrent(
      String action, String resourceType, Long resourceId, Map<String, ?> details) {
    audit(currentUserProvider.requireCurrentUser().id(), action, resourceType, resourceId, details);
  }

  private Map<String, Long> countPostsByStatus() {
    return toCountMap(postMapper.countByStatus());
  }

  private Map<String, Long> countUsersByStatus() {
    return toCountMap(userMapper.countByStatus());
  }

  private Map<String, Long> countSensitiveHitsByLevel() {
    return toCountMap(sensitiveWordHitMapper.countByLevel());
  }

  private long countPostsByStatus(String status) {
    return postMapper.selectCount(new LambdaQueryWrapper<PostEntity>().eq(PostEntity::getStatus, status));
  }

  private void requirePostScope(CurrentUser operator, PostEntity post) {
    if (ROLE_ADMIN.equals(operator.role())) {
      return;
    }
    if (ROLE_MODERATOR.equals(operator.role())
        && moderatorMapper.countByCategoryAndUser(post.getCategoryId(), operator.id()) > 0) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "Forbidden");
  }

  private PostDto toPostDto(PostEntity post) {
    UserEntity author = requireUser(post.getAuthorId());
    CategoryEntity category = requireCategory(post.getCategoryId());
    return new PostDto(
        String.valueOf(post.getId()),
        post.getTitle(),
        post.getSummary(),
        post.getContent(),
        post.getCoverUrl(),
        UserDto.from(author),
        CategoryDto.from(category),
        listTags(post.getId()),
        post.getStatus(),
        defaultInt(post.getViewCount()),
        defaultInt(post.getLikeCount()),
        defaultInt(post.getFavoriteCount()),
        defaultInt(post.getCommentCount()),
        post.getCreatedAt(),
        post.getUpdatedAt(),
        post.getPublishedAt());
  }

  private List<String> listTags(Long postId) {
    return postTagMapper
        .selectList(
            new LambdaQueryWrapper<PostTagEntity>()
                .eq(PostTagEntity::getPostId, postId)
                .orderByAsc(PostTagEntity::getId))
        .stream()
        .map(PostTagEntity::getTag)
        .toList();
  }

  private UserEntity requireUser(Long id) {
    UserEntity user = userMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
    }
    return user;
  }

  private CategoryEntity requireCategory(Long id) {
    CategoryEntity category = categoryMapper.selectById(id);
    if (category == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Category not found");
    }
    return category;
  }

  private PostEntity requirePost(Long id) {
    PostEntity post = postMapper.selectById(id);
    if (post == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return post;
  }

  private SensitiveWordEntity requireSensitiveWord(Long id) {
    SensitiveWordEntity entity = sensitiveWordMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Sensitive word not found");
    }
    return entity;
  }

  private void ensureSensitiveWordAvailable(String word, Long currentId) {
    LambdaQueryWrapper<SensitiveWordEntity> query =
        new LambdaQueryWrapper<SensitiveWordEntity>().eq(SensitiveWordEntity::getWord, word);
    if (currentId != null) {
      query.ne(SensitiveWordEntity::getId, currentId);
    }
    if (sensitiveWordMapper.selectCount(query) > 0) {
      throw duplicateSensitiveWord();
    }
  }

  private BusinessException duplicateSensitiveWord() {
    return new BusinessException(ErrorCode.DUPLICATE, "Sensitive word already exists");
  }

  private String normalizeWord(String word) {
    String normalized = normalizeRequired(word, "Word is required").toLowerCase(Locale.ROOT);
    if (normalized.length() > 100) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Word is too long");
    }
    return normalized;
  }

  private String normalizeLevel(String level) {
    String normalized = normalizeRequired(level, "Level is required").toLowerCase(Locale.ROOT);
    if (!"low".equals(normalized) && !"medium".equals(normalized) && !"high".equals(normalized)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported sensitive word level");
    }
    return normalized;
  }

  private String normalizeRequired(String value, String message) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
    return normalized;
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private String defaultReason(String reason, String fallback) {
    String normalized = reason == null ? null : reason.trim();
    return normalized == null || normalized.isEmpty() ? fallback : normalized;
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  private String toJson(Map<String, ?> details) {
    try {
      return objectMapper.writeValueAsString(details);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(ErrorCode.SERVER_ERROR, "Audit log detail is invalid");
    }
  }

  private Map<String, Long> toCountMap(List<AdminCountGroupDto> rows) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (AdminCountGroupDto row : rows) {
      counts.put(row.getGroupKey(), row.getCount() == null ? 0L : row.getCount());
    }
    return counts;
  }

  private Map<String, Object> details(Object... keysAndValues) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (int index = 0; index + 1 < keysAndValues.length; index += 2) {
      details.put((String) keysAndValues[index], keysAndValues[index + 1]);
    }
    return details;
  }
}
