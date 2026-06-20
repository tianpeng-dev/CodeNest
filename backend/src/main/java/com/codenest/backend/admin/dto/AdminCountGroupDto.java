package com.codenest.backend.admin.dto;

public class AdminCountGroupDto {
  private String groupKey;
  private Long count;

  public String getGroupKey() {
    return groupKey;
  }

  public void setGroupKey(String groupKey) {
    this.groupKey = groupKey;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }
}
