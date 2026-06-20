package com.codenest.backend.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenest.backend.admin.dto.AdminCountGroupDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {
  @Select("SELECT status AS group_key, COUNT(*) AS count FROM posts GROUP BY status")
  List<AdminCountGroupDto> countByStatus();

  @Update("UPDATE posts SET like_count = like_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int incrementLikeCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END,"
          + " updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int decrementLikeCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET dislike_count = dislike_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int incrementDislikeCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET dislike_count = CASE WHEN dislike_count > 0 THEN dislike_count - 1 ELSE 0 END,"
          + " updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int decrementDislikeCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET favorite_count = favorite_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int incrementFavoriteCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END,"
          + " updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int decrementFavoriteCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET comment_count = comment_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int incrementCommentCount(@Param("postId") Long postId);

  @Update(
      "UPDATE posts SET comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END,"
          + " updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
  int decrementCommentCount(@Param("postId") Long postId);
}
