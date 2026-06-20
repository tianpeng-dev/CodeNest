package com.codenest.backend.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {
  @Update(
      "UPDATE messages SET read_at = CURRENT_TIMESTAMP"
          + " WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND read_at IS NULL")
  int markIncomingRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
