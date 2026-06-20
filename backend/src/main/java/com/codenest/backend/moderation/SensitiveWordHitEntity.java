package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sensitive_word_hits")
public class SensitiveWordHitEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long wordId;
  private String resourceType;
  private Long resourceId;
  private Long userId;
  private String level;
  private String snippet;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getWordId() { return wordId; }
  public void setWordId(Long wordId) { this.wordId = wordId; }
  public String getResourceType() { return resourceType; }
  public void setResourceType(String resourceType) { this.resourceType = resourceType; }
  public Long getResourceId() { return resourceId; }
  public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getSnippet() { return snippet; }
  public void setSnippet(String snippet) { this.snippet = snippet; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
