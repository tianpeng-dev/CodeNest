package com.codenest.backend.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
  @Update(
      "UPDATE comments SET status = 'deleted', updated_at = CURRENT_TIMESTAMP"
          + " WHERE id = #{id} AND status = 'visible'")
  int markDeletedIfVisible(@Param("id") Long id);
}
