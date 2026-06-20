package com.codenest.backend.admin;

import com.codenest.backend.admin.dto.AdminModeratorRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminCategoryModeratorMapper {
  @Select("SELECT category_id FROM category_moderators WHERE user_id = #{userId}")
  List<Long> selectCategoryIdsByUserId(@Param("userId") Long userId);

  @Select("SELECT COUNT(*) FROM category_moderators WHERE user_id = #{userId}")
  long countByUserId(@Param("userId") Long userId);

  @Select("SELECT COUNT(*) FROM category_moderators WHERE category_id = #{categoryId} AND user_id = #{userId}")
  long countByCategoryAndUser(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

  @Insert(
      "INSERT INTO category_moderators (category_id, user_id, assigned_by, created_at, updated_at)"
          + " VALUES (#{categoryId}, #{userId}, #{assignedBy}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
  int insertAssignment(
      @Param("categoryId") Long categoryId,
      @Param("userId") Long userId,
      @Param("assignedBy") Long assignedBy);

  @Delete("DELETE FROM category_moderators WHERE id = #{id}")
  int deleteAssignment(@Param("id") Long id);

  @Select("SELECT user_id FROM category_moderators WHERE id = #{id}")
  Long selectUserIdById(@Param("id") Long id);

  @Select(
      """
      SELECT
        cm.id,
        cm.category_id,
        c.name AS category_name,
        c.description AS category_description,
        c.updated_at AS category_updated_at,
        u.id AS user_id,
        u.username,
        u.display_name,
        u.status,
        u.avatar_url
      FROM category_moderators cm
      JOIN categories c ON c.id = cm.category_id
      JOIN users u ON u.id = cm.user_id
      ORDER BY c.sort_order ASC, c.id ASC, cm.id ASC
      """)
  List<AdminModeratorRow> selectModeratorRows();
}
