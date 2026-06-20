package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenest.backend.admin.dto.AdminCountGroupDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SensitiveWordHitMapper extends BaseMapper<SensitiveWordHitEntity> {
  @Select("SELECT level AS group_key, COUNT(*) AS count FROM sensitive_word_hits GROUP BY level")
  List<AdminCountGroupDto> countByLevel();
}
