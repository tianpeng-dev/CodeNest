package com.codenest.backend.category;

import com.codenest.backend.category.dto.CategoryDto;
import com.codenest.backend.category.dto.CreateCategoryRequest;
import com.codenest.backend.category.dto.UpdateCategoryRequest;
import com.codenest.backend.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {
  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping("/categories")
  public ApiResponse<List<CategoryDto>> listActive() {
    return ApiResponse.ok(categoryService.listActive());
  }

  @GetMapping("/admin/categories")
  public ApiResponse<List<CategoryDto>> listAllForAdmin() {
    return ApiResponse.ok(categoryService.listAllForAdmin());
  }

  @PostMapping("/admin/categories")
  public ApiResponse<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest request) {
    return ApiResponse.ok(categoryService.create(request));
  }

  @PutMapping("/admin/categories/{id}")
  public ApiResponse<CategoryDto> update(
      @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
    return ApiResponse.ok(categoryService.update(id, request));
  }

  @DeleteMapping("/admin/categories/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ApiResponse.ok(null);
  }
}
