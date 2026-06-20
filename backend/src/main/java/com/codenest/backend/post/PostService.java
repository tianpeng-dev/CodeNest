package com.codenest.backend.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.category.CategoryEntity;
import com.codenest.backend.category.CategoryMapper;
import com.codenest.backend.category.dto.CategoryDto;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import com.codenest.backend.common.PageResult;
import com.codenest.backend.post.dto.PostDraftRequest;
import com.codenest.backend.post.dto.PostDto;
import com.codenest.backend.post.dto.PostQuery;
import com.codenest.backend.security.CurrentUser;
import com.codenest.backend.security.CurrentUserProvider;
import com.codenest.backend.security.PermissionService;
import com.codenest.backend.user.UserEntity;
import com.codenest.backend.user.UserMapper;
import com.codenest.backend.user.dto.UserDto;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService extends ServiceImpl<PostMapper, PostEntity> {
  private static final String STATUS_DRAFT = "draft";
  private static final String STATUS_PUBLISHED = "published";
  private static final String STATUS_DELETED = "deleted";
  private static final String REACTION_LIKE = "like";
  private static final String REACTION_DISLIKE = "dislike";

  private final PostTagMapper postTagMapper;
  private final PostReactionMapper postReactionMapper;
  private final FavoriteMapper favoriteMapper;
  private final UserMapper userMapper;
  private final CategoryMapper categoryMapper;
  private final CurrentUserProvider currentUserProvider;
  private final PermissionService permissionService;

  public PostService(
      PostTagMapper postTagMapper,
      PostReactionMapper postReactionMapper,
      FavoriteMapper favoriteMapper,
      UserMapper userMapper,
      CategoryMapper categoryMapper,
      CurrentUserProvider currentUserProvider,
      PermissionService permissionService) {
    this.postTagMapper = postTagMapper;
    this.postReactionMapper = postReactionMapper;
    this.favoriteMapper = favoriteMapper;
    this.userMapper = userMapper;
    this.categoryMapper = categoryMapper;
    this.currentUserProvider = currentUserProvider;
    this.permissionService = permissionService;
  }

  public PageResult<PostDto> listPublic(PostQuery query) {
    PostQuery effectiveQuery =
        new PostQuery(
            query.keyword(),
            query.categoryId(),
            query.categorySlug(),
            query.authorId(),
            query.tags(),
            STATUS_PUBLISHED,
            query.page(),
            query.pageSize(),
            query.sortBy());
    return listPosts(effectiveQuery, null, true);
  }

  public PageResult<PostDto> listCreator(PostQuery query) {
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    return listPosts(query, currentUser.id(), false);
  }

  public PostDto getPublic(Long id) {
    PostEntity post = requirePost(id);
    if (STATUS_PUBLISHED.equals(post.getStatus())) {
      return toDto(post);
    }

    CurrentUser currentUser = optionalCurrentUser();
    if (canManage(post, currentUser)) {
      return toDto(post);
    }
    throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
  }

  @Transactional
  public PostDto create(PostDraftRequest request) {
    UserEntity author = currentUserProvider.requireCurrentUserEntity();
    CategoryEntity category = requireActiveCategory(request.categoryId());
    String status = normalizeRequestedStatus(request.status());
    validateContentAllowed(request.title(), request.summary(), request.content());

    LocalDateTime now = LocalDateTime.now();
    PostEntity post = new PostEntity();
    post.setAuthorId(author.getId());
    post.setCategoryId(category.getId());
    post.setTitle(trimRequired(request.title(), "Title is required"));
    post.setSummary(defaultString(request.summary()));
    post.setContent(trimRequired(request.content(), "Content is required"));
    post.setCoverUrl(defaultString(request.coverUrl()));
    post.setStatus(status);
    post.setViewCount(0);
    post.setLikeCount(0);
    post.setDislikeCount(0);
    post.setFavoriteCount(0);
    post.setCommentCount(0);
    post.setPublishedAt(STATUS_PUBLISHED.equals(status) ? now : null);
    post.setCreatedAt(now);
    post.setUpdatedAt(now);
    save(post);
    replaceTags(post.getId(), request.tags(), now);
    return toDto(post);
  }

  @Transactional
  public PostDto update(Long id, PostDraftRequest request) {
    PostEntity post = requirePost(id);
    requireManagePermission(post);
    requireNotDeleted(post);
    requireActiveCategory(request.categoryId());

    String previousStatus = post.getStatus();
    String status = normalizeRequestedStatus(request.status());
    validateContentAllowed(request.title(), request.summary(), request.content());

    post.setTitle(trimRequired(request.title(), "Title is required"));
    post.setSummary(defaultString(request.summary()));
    post.setContent(trimRequired(request.content(), "Content is required"));
    post.setCoverUrl(defaultString(request.coverUrl()));
    post.setCategoryId(request.categoryId());
    post.setStatus(status);
    if (!STATUS_PUBLISHED.equals(previousStatus)
        && STATUS_PUBLISHED.equals(status)
        && post.getPublishedAt() == null) {
      post.setPublishedAt(LocalDateTime.now());
    }
    post.setUpdatedAt(LocalDateTime.now());
    updateById(post);
    replaceTags(post.getId(), request.tags(), LocalDateTime.now());
    return toDto(post);
  }

  @Transactional
  public PostDto publish(Long id) {
    PostEntity post = requirePost(id);
    requireManagePermission(post);
    if (STATUS_DELETED.equals(post.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Deleted post cannot be published");
    }

    LocalDateTime now = LocalDateTime.now();
    post.setStatus(STATUS_PUBLISHED);
    if (post.getPublishedAt() == null) {
      post.setPublishedAt(now);
    }
    post.setUpdatedAt(now);
    updateById(post);
    return toDto(post);
  }

  @Transactional
  public void softDelete(Long id) {
    PostEntity post = requirePost(id);
    requireManagePermission(post);
    post.setStatus(STATUS_DELETED);
    post.setUpdatedAt(LocalDateTime.now());
    updateById(post);
  }

  @Transactional
  public PostDto toggleLike(Long id) {
    return toggleReaction(id, REACTION_LIKE);
  }

  @Transactional
  public PostDto toggleDislike(Long id) {
    return toggleReaction(id, REACTION_DISLIKE);
  }

  @Transactional
  public PostDto toggleFavorite(Long id) {
    PostEntity post = requirePublishedPost(id);
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    FavoriteEntity favorite = findFavorite(post.getId(), currentUser.id());
    if (favorite == null) {
      FavoriteEntity created = new FavoriteEntity();
      created.setPostId(post.getId());
      created.setUserId(currentUser.id());
      created.setCreatedAt(LocalDateTime.now());
      try {
        favoriteMapper.insert(created);
        baseMapper.incrementFavoriteCount(post.getId());
      } catch (DuplicateKeyException exception) {
        return toDto(requirePost(post.getId()));
      }
    } else {
      if (favoriteMapper.deleteById(favorite.getId()) > 0) {
        baseMapper.decrementFavoriteCount(post.getId());
      }
    }

    return toDto(requirePost(post.getId()));
  }

  private PageResult<PostDto> listPosts(PostQuery query, Long forcedAuthorId, boolean publicOnly) {
    LambdaQueryWrapper<PostEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(publicOnly, PostEntity::getStatus, STATUS_PUBLISHED);
    if (!publicOnly && hasText(query.status())) {
      wrapper.eq(PostEntity::getStatus, query.status().trim());
    }
    if (forcedAuthorId != null) {
      wrapper.eq(PostEntity::getAuthorId, forcedAuthorId);
    } else if (query.authorId() != null) {
      wrapper.eq(PostEntity::getAuthorId, query.authorId());
    }
    if (query.categoryId() != null) {
      wrapper.eq(PostEntity::getCategoryId, query.categoryId());
    }
    Long categoryId = findCategoryIdBySlug(query.categorySlug());
    if (categoryId != null) {
      wrapper.eq(PostEntity::getCategoryId, categoryId);
    }
    applyKeyword(wrapper, query.keyword());
    applyTagFilter(wrapper, query.tags());
    applySort(wrapper, query.sortBy());

    Page<PostEntity> page = new Page<>(query.normalizedPage(), query.normalizedPageSize());
    Page<PostEntity> result = page(page, wrapper);
    return new PageResult<>(
        result.getRecords().stream().map(this::toDto).toList(),
        result.getTotal(),
        query.normalizedPage(),
        query.normalizedPageSize());
  }

  private PostDto toggleReaction(Long id, String targetReaction) {
    PostEntity post = requirePublishedPost(id);
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    PostReactionEntity reaction = findReaction(post.getId(), currentUser.id());
    if (reaction == null) {
      if (createReaction(post.getId(), currentUser.id(), targetReaction)) {
        incrementReactionCount(post.getId(), targetReaction);
      }
    } else if (targetReaction.equals(reaction.getReaction())) {
      if (postReactionMapper.deleteById(reaction.getId()) > 0) {
        decrementReactionCount(post.getId(), targetReaction);
      }
    } else {
      String previousReaction = reaction.getReaction();
      if (postReactionMapper.updateReactionIfCurrent(
              post.getId(), currentUser.id(), previousReaction, targetReaction)
          > 0) {
        decrementReactionCount(post.getId(), previousReaction);
        incrementReactionCount(post.getId(), targetReaction);
      }
    }

    return toDto(requirePost(post.getId()));
  }

  private boolean createReaction(Long postId, Long userId, String reactionType) {
    LocalDateTime now = LocalDateTime.now();
    PostReactionEntity reaction = new PostReactionEntity();
    reaction.setPostId(postId);
    reaction.setUserId(userId);
    reaction.setReaction(reactionType);
    reaction.setCreatedAt(now);
    reaction.setUpdatedAt(now);
    try {
      return postReactionMapper.insert(reaction) > 0;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }

  private void incrementReactionCount(Long postId, String reaction) {
    if (REACTION_LIKE.equals(reaction)) {
      baseMapper.incrementLikeCount(postId);
      return;
    }
    baseMapper.incrementDislikeCount(postId);
  }

  private void decrementReactionCount(Long postId, String reaction) {
    if (REACTION_LIKE.equals(reaction)) {
      baseMapper.decrementLikeCount(postId);
      return;
    }
    baseMapper.decrementDislikeCount(postId);
  }

  private void replaceTags(Long postId, List<String> rawTags, LocalDateTime now) {
    postTagMapper.delete(new LambdaQueryWrapper<PostTagEntity>().eq(PostTagEntity::getPostId, postId));
    for (String tag : normalizeTags(rawTags)) {
      PostTagEntity entity = new PostTagEntity();
      entity.setPostId(postId);
      entity.setTag(tag);
      entity.setCreatedAt(now);
      postTagMapper.insert(entity);
    }
  }

  private List<String> normalizeTags(List<String> rawTags) {
    if (rawTags == null) {
      return List.of();
    }
    LinkedHashSet<String> tags = new LinkedHashSet<>();
    for (String rawTag : rawTags) {
      String tag = normalizeTag(rawTag);
      if (tag != null) {
        if (tag.length() > 40) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "Tag is too long");
        }
        tags.add(tag);
      }
      if (tags.size() >= 10) {
        break;
      }
    }
    return List.copyOf(tags);
  }

  private void applyKeyword(LambdaQueryWrapper<PostEntity> wrapper, String keyword) {
    String text = trimToNull(keyword);
    if (text == null) {
      return;
    }
    wrapper.and(
        nested ->
            nested
                .like(PostEntity::getTitle, text)
                .or()
                .like(PostEntity::getSummary, text)
                .or()
                .like(PostEntity::getContent, text));
  }

  private void applyTagFilter(LambdaQueryWrapper<PostEntity> wrapper, String tags) {
    Set<String> requestedTags =
        normalizeTags(hasText(tags) ? List.of(tags.split(",")) : List.of())
            .stream()
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (requestedTags.isEmpty()) {
      return;
    }

    List<PostTagEntity> tagRows =
        postTagMapper.selectList(
            new LambdaQueryWrapper<PostTagEntity>().in(PostTagEntity::getTag, requestedTags));
    Set<Long> postIds =
        tagRows.stream()
            .collect(Collectors.groupingBy(PostTagEntity::getPostId))
            .entrySet()
            .stream()
            .filter(entry -> tagsContainAll(entry.getValue(), requestedTags))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    if (postIds.isEmpty()) {
      wrapper.eq(PostEntity::getId, -1L);
      return;
    }
    wrapper.in(PostEntity::getId, postIds);
  }

  private void applySort(LambdaQueryWrapper<PostEntity> wrapper, String sortBy) {
    String sort = hasText(sortBy) ? sortBy.trim() : "latest";
    switch (sort) {
      case "popular" -> wrapper.orderByDesc(PostEntity::getViewCount).orderByDesc(PostEntity::getLikeCount);
      case "commented" -> wrapper.orderByDesc(PostEntity::getCommentCount).orderByDesc(PostEntity::getId);
      case "latest" -> wrapper.orderByDesc(PostEntity::getPublishedAt).orderByDesc(PostEntity::getId);
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported sortBy");
    }
  }

  private Long findCategoryIdBySlug(String slug) {
    String categorySlug = trimToNull(slug);
    if (categorySlug == null) {
      return null;
    }
    CategoryEntity category =
        categoryMapper.selectOne(
            new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getSlug, categorySlug));
    if (category == null) {
      return -1L;
    }
    return category.getId();
  }

  private boolean tagsContainAll(List<PostTagEntity> tagRows, Set<String> requestedTags) {
    return tagRows.stream().map(PostTagEntity::getTag).collect(Collectors.toSet()).containsAll(requestedTags);
  }

  private PostEntity requirePublishedPost(Long id) {
    PostEntity post = requirePost(id);
    if (!STATUS_PUBLISHED.equals(post.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return post;
  }

  private PostEntity requirePost(Long id) {
    PostEntity post = getById(id);
    if (post == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
    }
    return post;
  }

  private CategoryEntity requireActiveCategory(Long categoryId) {
    CategoryEntity category = categoryMapper.selectById(categoryId);
    if (category == null || !"active".equals(category.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Category is invalid");
    }
    return category;
  }

  private void requireNotDeleted(PostEntity post) {
    if (STATUS_DELETED.equals(post.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Deleted post cannot be updated");
    }
  }

  private void requireManagePermission(PostEntity post) {
    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    if (!canManage(post, currentUser)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Forbidden");
    }
  }

  private boolean canManage(PostEntity post, CurrentUser currentUser) {
    return currentUser != null
        && (Objects.equals(post.getAuthorId(), currentUser.id()) || permissionService.isAdmin(currentUser));
  }

  private CurrentUser optionalCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt)) {
      return null;
    }
    return currentUserProvider.requireCurrentUser();
  }

  private PostReactionEntity findReaction(Long postId, Long userId) {
    return postReactionMapper.selectOne(
        new LambdaQueryWrapper<PostReactionEntity>()
            .eq(PostReactionEntity::getPostId, postId)
            .eq(PostReactionEntity::getUserId, userId));
  }

  private FavoriteEntity findFavorite(Long postId, Long userId) {
    return favoriteMapper.selectOne(
        new LambdaQueryWrapper<FavoriteEntity>()
            .eq(FavoriteEntity::getPostId, postId)
            .eq(FavoriteEntity::getUserId, userId));
  }

  private PostDto toDto(PostEntity post) {
    UserEntity author = userMapper.selectById(post.getAuthorId());
    CategoryEntity category = categoryMapper.selectById(post.getCategoryId());
    if (author == null || category == null) {
      throw new BusinessException(ErrorCode.SERVER_ERROR, "Post relation is invalid");
    }
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

  private String normalizeRequestedStatus(String status) {
    String normalized = hasText(status) ? status.trim() : STATUS_DRAFT;
    if (!STATUS_DRAFT.equals(normalized) && !STATUS_PUBLISHED.equals(normalized)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported post status");
    }
    return normalized;
  }

  private void validateContentAllowed(String title, String summary, String content) {
    trimRequired(title, "Title is required");
    trimRequired(content, "Content is required");
  }

  private String trimRequired(String value, String message) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
    return trimmed;
  }

  private String defaultString(String value) {
    return value == null ? "" : value.trim();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeTag(String value) {
    String tag = trimToNull(value);
    return tag == null ? null : tag.toLowerCase(Locale.ROOT);
  }

  private boolean hasText(String value) {
    return trimToNull(value) != null;
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }
}
