package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWordEntity> {
  @Update(
      "UPDATE sensitive_words SET hit_count = hit_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int incrementHitCount(@Param("id") Long id);
}
