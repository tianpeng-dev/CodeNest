package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordService extends ServiceImpl<SensitiveWordMapper, SensitiveWordEntity> {
  private static final String HIGH_LEVEL = "high";

  public void assertPublishAllowed(String title, String summary, String content) {
    String combined = normalize(title) + "\n" + normalize(summary) + "\n" + normalize(content);
    for (SensitiveWordEntity word : highSensitiveWords()) {
      String needle = normalize(word.getWord());
      if (!needle.isEmpty() && combined.contains(needle)) {
        throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED, "Content contains blocked words");
      }
    }
  }

  private List<SensitiveWordEntity> highSensitiveWords() {
    return list(
        new LambdaQueryWrapper<SensitiveWordEntity>().eq(SensitiveWordEntity::getLevel, HIGH_LEVEL));
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }
}
