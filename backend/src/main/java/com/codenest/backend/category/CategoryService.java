package com.codenest.backend.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.admin.AdminService;
import com.codenest.backend.category.dto.CategoryDto;
import com.codenest.backend.category.dto.CreateCategoryRequest;
import com.codenest.backend.category.dto.UpdateCategoryRequest;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService extends ServiceImpl<CategoryMapper, CategoryEntity> {
  private static final String ACTIVE_STATUS = "active";
  private static final String DISABLED_STATUS = "disabled";

  private final AdminService adminService;

  public CategoryService(AdminService adminService) {
    this.adminService = adminService;
  }

  public List<CategoryDto> listActive() {
    return list(baseQuery().eq(CategoryEntity::getStatus, ACTIVE_STATUS)).stream()
        .map(CategoryDto::from)
        .toList();
  }

  public List<CategoryDto> listAllForAdmin() {
    return list(baseQuery()).stream().map(CategoryDto::from).toList();
  }

  @Transactional
  public CategoryDto create(CreateCategoryRequest request) {
    ensureSlugAvailable(request.slug(), null);

    LocalDateTime now = LocalDateTime.now();
    CategoryEntity category = new CategoryEntity();
    category.setName(request.name().trim());
    category.setSlug(request.slug());
    category.setDescription(defaultString(request.description()));
    category.setCoverUrl(defaultString(request.coverUrl()));
    category.setSortOrder(defaultInt(request.sortOrder()));
    category.setStatus(defaultStatus(request.status()));
    category.setPostCount(0);
    category.setCreatedAt(now);
    category.setUpdatedAt(now);

    try {
      save(category);
    } catch (DuplicateKeyException exception) {
      throw duplicateSlugException();
    }
    adminService.auditCurrent(
        "category.create",
        "category",
        category.getId(),
        Map.of("name", category.getName(), "slug", category.getSlug(), "status", category.getStatus()));
    return CategoryDto.from(category);
  }

  @Transactional
  public CategoryDto update(Long id, UpdateCategoryRequest request) {
    CategoryEntity category = requireCategory(id);
    ensureSlugAvailable(request.slug(), id);

    category.setName(request.name().trim());
    category.setSlug(request.slug());
    category.setDescription(defaultString(request.description()));
    category.setCoverUrl(defaultString(request.coverUrl()));
    category.setSortOrder(defaultInt(request.sortOrder()));
    category.setStatus(defaultStatus(request.status()));
    category.setUpdatedAt(LocalDateTime.now());

    try {
      updateById(category);
    } catch (DuplicateKeyException exception) {
      throw duplicateSlugException();
    }
    adminService.auditCurrent(
        "category.update",
        "category",
        category.getId(),
        Map.of("name", category.getName(), "slug", category.getSlug(), "status", category.getStatus()));
    return CategoryDto.from(category);
  }

  @Transactional
  public void deleteCategory(Long id) {
    CategoryEntity category = requireCategory(id);
    if (defaultInt(category.getPostCount()) > 0) {
      category.setStatus(DISABLED_STATUS);
      category.setUpdatedAt(LocalDateTime.now());
      updateById(category);
      adminService.auditCurrent(
          "category.disable",
          "category",
          category.getId(),
          Map.of("slug", category.getSlug(), "postCount", defaultInt(category.getPostCount())));
      return;
    }

    removeById(id);
    adminService.auditCurrent(
        "category.delete",
        "category",
        id,
        Map.of("slug", category.getSlug(), "postCount", defaultInt(category.getPostCount())));
  }

  private CategoryEntity requireCategory(Long id) {
    CategoryEntity category = getById(id);
    if (category == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Category not found");
    }
    return category;
  }

  private void ensureSlugAvailable(String slug, Long currentCategoryId) {
    LambdaQueryWrapper<CategoryEntity> query =
        new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getSlug, slug);
    if (currentCategoryId != null) {
      query.ne(CategoryEntity::getId, currentCategoryId);
    }
    if (count(query) > 0) {
      throw duplicateSlugException();
    }
  }

  private LambdaQueryWrapper<CategoryEntity> baseQuery() {
    return new LambdaQueryWrapper<CategoryEntity>()
        .orderByAsc(CategoryEntity::getSortOrder)
        .orderByAsc(CategoryEntity::getId);
  }

  private BusinessException duplicateSlugException() {
    return new BusinessException(ErrorCode.DUPLICATE, "Category slug already exists");
  }

  private String defaultString(String value) {
    return value == null ? "" : value;
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  private String defaultStatus(String value) {
    return value == null ? ACTIVE_STATUS : value;
  }
}
