package com.codenest.backend.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {
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
}
