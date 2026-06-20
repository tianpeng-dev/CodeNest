package com.codenest.backend.admin.dto;

import java.time.LocalDateTime;

public class AdminModeratorRow {
  private Long id;
  private Long categoryId;
  private String categoryName;
  private String categoryDescription;
  private LocalDateTime categoryUpdatedAt;
  private Long userId;
  private String username;
  private String displayName;
  private String status;
  private String avatarUrl;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public String getCategoryDescription() {
    return categoryDescription;
  }

  public void setCategoryDescription(String categoryDescription) {
    this.categoryDescription = categoryDescription;
  }

  public LocalDateTime getCategoryUpdatedAt() {
    return categoryUpdatedAt;
  }

  public void setCategoryUpdatedAt(LocalDateTime categoryUpdatedAt) {
    this.categoryUpdatedAt = categoryUpdatedAt;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }
}
