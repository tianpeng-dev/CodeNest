package com.codenest.backend.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
  @Update(
      "UPDATE users SET follower_count = follower_count + 1, updated_at = CURRENT_TIMESTAMP"
          + " WHERE id = #{userId}")
  int incrementFollowerCount(@Param("userId") Long userId);

  @Update(
      "UPDATE users SET follower_count = CASE WHEN follower_count > 0 THEN follower_count - 1 ELSE 0 END,"
          + " updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
  int decrementFollowerCount(@Param("userId") Long userId);
}
