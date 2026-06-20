package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SensitiveWordHitMapper extends BaseMapper<SensitiveWordHitEntity> {}
