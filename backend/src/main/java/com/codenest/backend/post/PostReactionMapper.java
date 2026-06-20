package com.codenest.backend.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostReactionMapper extends BaseMapper<PostReactionEntity> {
  @Update(
      "UPDATE post_reactions SET reaction = #{nextReaction}, updated_at = CURRENT_TIMESTAMP"
          + " WHERE post_id = #{postId} AND user_id = #{userId} AND reaction = #{currentReaction}")
  int updateReactionIfCurrent(
      @Param("postId") Long postId,
      @Param("userId") Long userId,
      @Param("currentReaction") String currentReaction,
      @Param("nextReaction") String nextReaction);

  @Delete(
      "DELETE FROM post_reactions"
          + " WHERE post_id = #{postId} AND user_id = #{userId} AND reaction = #{reaction}")
  int deleteReactionIfCurrent(
      @Param("postId") Long postId,
      @Param("userId") Long userId,
      @Param("reaction") String reaction);
}
