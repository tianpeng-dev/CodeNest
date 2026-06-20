package com.codenest.backend.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryModeratorMapper {
  @Select(
      "SELECT COUNT(*) > 0 FROM category_moderators"
          + " WHERE category_id = #{categoryId} AND user_id = #{userId}")
  boolean existsAssignment(@Param("categoryId") Long categoryId, @Param("userId") Long userId);
}
