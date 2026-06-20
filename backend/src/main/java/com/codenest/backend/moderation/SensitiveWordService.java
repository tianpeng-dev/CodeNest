package com.codenest.backend.moderation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenest.backend.common.BusinessException;
import com.codenest.backend.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SensitiveWordService extends ServiceImpl<SensitiveWordMapper, SensitiveWordEntity> {
  private static final String NONE_LEVEL = "none";
  private static final String LOW_LEVEL = "low";
  private static final String MEDIUM_LEVEL = "medium";
  private static final String HIGH_LEVEL = "high";
  private static final int SNIPPET_RADIUS = 40;
  private static final int MAX_SNIPPET_LENGTH = 300;

  private final SensitiveWordHitMapper sensitiveWordHitMapper;
  private final TransactionTemplate requiresNewTransaction;

  public SensitiveWordService(
      SensitiveWordHitMapper sensitiveWordHitMapper, PlatformTransactionManager transactionManager) {
    this.sensitiveWordHitMapper = sensitiveWordHitMapper;
    this.requiresNewTransaction = new TransactionTemplate(transactionManager);
    this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void assertPublishAllowed(String title, String summary, String content) {
    blockIfHigh(scan(title, summary, content), "post", null, null);
  }

  public ScanResult scan(String... parts) {
    String combined = String.join("\n", nullSafeParts(parts));
    String normalizedCombined = normalizeText(combined);
    List<Hit> hits =
        list().stream()
            .map(word -> toHit(word, combined, normalizedCombined))
            .filter(hit -> hit != null)
            .sorted(Comparator.comparing(hit -> hit.word().getId()))
            .toList();
    String maxLevel =
        hits.stream().map(Hit::level).max(Comparator.comparingInt(this::severity)).orElse(NONE_LEVEL);
    return new ScanResult(maxLevel, hits);
  }

  public void blockIfHigh(ScanResult scanResult, String resourceType, Long resourceId, Long userId) {
    if (!scanResult.high()) {
      return;
    }
    recordHits(scanResult, resourceType, resourceId, userId);
    throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED, "Content contains blocked words");
  }

  public void recordHits(ScanResult scanResult, String resourceType, Long resourceId, Long userId) {
    if (scanResult.none() || userId == null) {
      return;
    }
    requiresNewTransaction.executeWithoutResult(
        status -> {
          LocalDateTime now = LocalDateTime.now();
          for (Hit hit : scanResult.hits()) {
            SensitiveWordHitEntity entity = new SensitiveWordHitEntity();
            entity.setWordId(hit.word().getId());
            entity.setResourceType(resourceType);
            entity.setResourceId(resourceId);
            entity.setUserId(userId);
            entity.setLevel(hit.level());
            entity.setSnippet(hit.snippet());
            entity.setCreatedAt(now);
            sensitiveWordHitMapper.insert(entity);
            baseMapper.incrementHitCount(hit.word().getId());
          }
        });
  }

  private Hit toHit(SensitiveWordEntity word, String combined, String normalizedCombined) {
    String needle = normalizeToken(word.getWord());
    if (needle.isEmpty()) {
      return null;
    }
    int index = normalizedCombined.indexOf(needle);
    if (index < 0) {
      return null;
    }
    String level = normalizeToken(word.getLevel());
    if (severity(level) == 0) {
      return null;
    }
    return new Hit(word, level, safeSnippet(combined, index, word.getWord().length()));
  }

  private String safeSnippet(String text, int matchIndex, int matchLength) {
    int start = Math.max(0, matchIndex - SNIPPET_RADIUS);
    int end = Math.min(text.length(), matchIndex + matchLength + SNIPPET_RADIUS);
    String prefix = start > 0 ? "..." : "";
    String suffix = end < text.length() ? "..." : "";
    String before = text.substring(start, Math.max(start, matchIndex));
    String after = text.substring(Math.min(text.length(), matchIndex + matchLength), end);
    String snippet = (prefix + before + "[redacted]" + after + suffix).replaceAll("\\s+", " ").trim();
    if (snippet.length() <= MAX_SNIPPET_LENGTH) {
      return snippet;
    }
    return snippet.substring(0, MAX_SNIPPET_LENGTH);
  }

  private int severity(String level) {
    return switch (level) {
      case LOW_LEVEL -> 1;
      case MEDIUM_LEVEL -> 2;
      case HIGH_LEVEL -> 3;
      default -> 0;
    };
  }

  private String[] nullSafeParts(String... parts) {
    if (parts == null) {
      return new String[] {""};
    }
    String[] safeParts = new String[parts.length];
    for (int index = 0; index < parts.length; index++) {
      safeParts[index] = parts[index] == null ? "" : parts[index];
    }
    return safeParts;
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private String normalizeToken(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
  }

  public record ScanResult(String maxLevel, List<Hit> hits) {
    public boolean none() {
      return hits.isEmpty();
    }

    public boolean high() {
      return HIGH_LEVEL.equals(maxLevel);
    }
  }

  public record Hit(SensitiveWordEntity word, String level, String snippet) {}
}
