package com.codenest.backend.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:comment_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class CommentIntegrationTest {
  private static final String OWNER_CLERK_ID = "clerk_comment_owner";
  private static final String OTHER_CLERK_ID = "clerk_comment_other";
  private static final String ADMIN_CLERK_ID = "clerk_comment_admin";
  private static final String MODERATOR_CLERK_ID = "clerk_comment_moderator";
  private static final String UNASSIGNED_MODERATOR_CLERK_ID = "clerk_comment_unassigned_moderator";
  private static final String BANNED_CLERK_ID = "clerk_comment_banned";
  private static final String MUTED_CLERK_ID = "clerk_comment_muted";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  private Long ownerId;
  private Long otherId;
  private Long adminId;
  private Long moderatorId;
  private Long categoryId;
  private Long postId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM sensitive_word_hits");
    jdbcTemplate.update("DELETE FROM comments");
    jdbcTemplate.update("DELETE FROM sensitive_words");
    jdbcTemplate.update("DELETE FROM favorites");
    jdbcTemplate.update("DELETE FROM post_reactions");
    jdbcTemplate.update("DELETE FROM post_tags");
    jdbcTemplate.update("DELETE FROM posts");
    jdbcTemplate.update("DELETE FROM category_moderators");
    jdbcTemplate.update("DELETE FROM categories");
    jdbcTemplate.update("DELETE FROM users");

    ownerId = insertUser(OWNER_CLERK_ID, "commentowner", "Comment Owner", "user", "active");
    otherId = insertUser(OTHER_CLERK_ID, "commentother", "Comment Other", "user", "active");
    adminId = insertUser(ADMIN_CLERK_ID, "commentadmin", "Comment Admin", "admin", "active");
    moderatorId =
        insertUser(MODERATOR_CLERK_ID, "commentmod", "Comment Moderator", "moderator", "active");
    insertUser(
        UNASSIGNED_MODERATOR_CLERK_ID,
        "commentunassignedmod",
        "Comment Unassigned Moderator",
        "moderator",
        "active");
    insertUser(BANNED_CLERK_ID, "commentbanned", "Comment Banned", "user", "banned");
    insertMutedUser();
    categoryId = insertCategory();
    postId = insertPost(ownerId, "published", 0);
  }

  @Test
  void publicListReturnsVisibleCommentsSortedAsc() throws Exception {
    insertComment(postId, otherId, "second visible", "visible", "2026-01-01 10:01:00");
    insertComment(postId, ownerId, "deleted comment", "deleted", "2026-01-01 10:02:00");
    insertComment(postId, ownerId, "first visible", "visible", "2026-01-01 10:00:00");

    mockMvc
        .perform(get("/posts/{postId}/comments", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].content").value("first visible"))
        .andExpect(jsonPath("$.data[0].id").isString())
        .andExpect(jsonPath("$.data[0].postId").value(String.valueOf(postId)))
        .andExpect(jsonPath("$.data[0].author.username").value("commentowner"))
        .andExpect(jsonPath("$.data[0].createdAt").exists())
        .andExpect(jsonPath("$.data[1].content").value("second visible"));
  }

  @Test
  void authenticatedUserCreatesCommentAndIncrementsPostCommentCount() throws Exception {
    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" Great discussion \"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").value("Great discussion"))
        .andExpect(jsonPath("$.data.author.username").value("commentother"));

    Integer commentCount =
        jdbcTemplate.queryForObject(
            "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(commentCount).isEqualTo(1);
  }

  @Test
  void highSensitiveWordBlocksCommentAndRecordsHit() throws Exception {
    insertSensitiveWord("hardblock", "high");

    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"This has hardblock inside\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40022));

    Integer comments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comments", Integer.class);
    Integer hits =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sensitive_word_hits WHERE resource_type = 'comment' AND resource_id IS NULL",
            Integer.class);
    assertThat(comments).isZero();
    assertThat(hits).isEqualTo(1);
  }

  @Test
  void lowAndMediumSensitiveWordsAllowCommentAndRecordHits() throws Exception {
    insertSensitiveWord("softwarn", "low");
    insertSensitiveWord("watchword", "medium");

    String response =
        mockMvc
            .perform(
                post("/posts/{postId}/comments", postId)
                    .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"softwarn and watchword are present\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long commentId = objectMapper.readTree(response).path("data").path("id").asLong();

    Integer hits =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sensitive_word_hits WHERE resource_type = 'comment' AND resource_id = ?",
            Integer.class,
            commentId);
    String snippet =
        jdbcTemplate.queryForObject(
            "SELECT snippet FROM sensitive_word_hits WHERE level = 'low'", String.class);
    Integer hitCount =
        jdbcTemplate.queryForObject(
            "SELECT SUM(hit_count) FROM sensitive_words WHERE word IN ('softwarn', 'watchword')",
            Integer.class);
    assertThat(hits).isEqualTo(2);
    assertThat(snippet).contains("[redacted]").doesNotContain("softwarn");
    assertThat(hitCount).isEqualTo(2);
  }

  @Test
  void bannedAndMutedUsersCannotComment() throws Exception {
    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId)
                .with(jwt().jwt(jwt -> jwt.subject(BANNED_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"blocked\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));

    mockMvc
        .perform(
            post("/posts/{postId}/comments", postId)
                .with(jwt().jwt(jwt -> jwt.subject(MUTED_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"also blocked\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));
  }

  @Test
  void ownerAndAdminCanDeleteCommentsButNonOwnerCannot() throws Exception {
    Long ownerCommentId = insertComment(postId, otherId, "owned comment", "visible", "2026-01-01 10:00:00");
    Long adminTargetId = insertComment(postId, otherId, "admin target", "visible", "2026-01-01 10:01:00");
    jdbcTemplate.update("UPDATE posts SET comment_count = 2 WHERE id = ?", postId);

    mockMvc
        .perform(
            delete("/comments/{id}", ownerCommentId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));

    mockMvc
        .perform(
            delete("/comments/{id}", ownerCommentId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    String status =
        jdbcTemplate.queryForObject(
            "SELECT status FROM comments WHERE id = ?", String.class, ownerCommentId);
    Integer commentCount =
        jdbcTemplate.queryForObject(
            "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(status).isEqualTo("deleted");
    assertThat(commentCount).isEqualTo(1);

    mockMvc
        .perform(
            delete("/comments/{id}", adminTargetId)
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk());

    Integer remainingCount =
        jdbcTemplate.queryForObject(
            "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(remainingCount).isZero();
  }

  @Test
  void repeatedDeleteDoesNotDoubleDecrementPostCommentCount() throws Exception {
    Long commentId =
        insertComment(postId, otherId, "delete once", "visible", "2026-01-01 10:00:00");
    jdbcTemplate.update("UPDATE posts SET comment_count = 1 WHERE id = ?", postId);

    mockMvc
        .perform(
            delete("/comments/{id}", commentId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            delete("/comments/{id}", commentId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk());

    Integer commentCount =
        jdbcTemplate.queryForObject(
            "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(commentCount).isZero();
  }

  @Test
  void assignedCategoryModeratorCanDeleteCommentButUnassignedModeratorCannot() throws Exception {
    Long assignedTargetId =
        insertComment(postId, otherId, "moderated", "visible", "2026-01-01 10:00:00");
    Long unassignedTargetId =
        insertComment(postId, otherId, "not moderated", "visible", "2026-01-01 10:01:00");
    insertCategoryModerator(categoryId, moderatorId, adminId);
    jdbcTemplate.update("UPDATE posts SET comment_count = 2 WHERE id = ?", postId);

    mockMvc
        .perform(
            delete("/comments/{id}", unassignedTargetId)
                .with(jwt().jwt(jwt -> jwt.subject(UNASSIGNED_MODERATOR_CLERK_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));

    mockMvc
        .perform(
            delete("/comments/{id}", assignedTargetId)
                .with(jwt().jwt(jwt -> jwt.subject(MODERATOR_CLERK_ID))))
        .andExpect(status().isOk());

    String status =
        jdbcTemplate.queryForObject(
            "SELECT status FROM comments WHERE id = ?", String.class, assignedTargetId);
    Integer commentCount =
        jdbcTemplate.queryForObject(
            "SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(status).isEqualTo("deleted");
    assertThat(commentCount).isEqualTo(1);
  }

  private Long insertUser(
      String clerkUserId, String username, String displayName, String role, String status) {
    jdbcTemplate.update(
        """
        INSERT INTO users (
          clerk_user_id,
          username,
          display_name,
          avatar_url,
          bio,
          role,
          status,
          created_at,
          updated_at
        ) VALUES (?, ?, ?, '', '', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        clerkUserId,
        username,
        displayName,
        role,
        status);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM users WHERE clerk_user_id = ?", Long.class, clerkUserId);
  }

  private void insertMutedUser() {
    insertUser(MUTED_CLERK_ID, "commentmuted", "Comment Muted", "user", "active");
    jdbcTemplate.update(
        "UPDATE users SET mute_until = TIMESTAMP '2099-01-01 00:00:00' WHERE clerk_user_id = ?",
        MUTED_CLERK_ID);
  }

  private Long insertCategory() {
    jdbcTemplate.update(
        """
        INSERT INTO categories (
          name,
          slug,
          description,
          sort_order,
          status,
          post_count,
          created_at,
          updated_at
        ) VALUES ('Comments', 'comments', '', 1, 'active', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE slug = 'comments'", Long.class);
  }

  private void insertCategoryModerator(Long categoryId, Long userId, Long assignedBy) {
    jdbcTemplate.update(
        """
        INSERT INTO category_moderators (
          category_id,
          user_id,
          assigned_by,
          created_at,
          updated_at
        ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        userId,
        assignedBy);
  }

  private Long insertPost(Long authorId, String status, int commentCount) {
    jdbcTemplate.update(
        """
        INSERT INTO posts (
          author_id,
          category_id,
          title,
          summary,
          content,
          cover_url,
          status,
          view_count,
          like_count,
          dislike_count,
          favorite_count,
          comment_count,
          published_at,
          created_at,
          updated_at
        ) VALUES (?, ?, 'Commentable', 'Summary', 'Content', '', ?, 0, 0, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        authorId,
        categoryId,
        status,
        commentCount);
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
  }

  private Long insertComment(
      Long postId, Long authorId, String content, String status, String createdAt) {
    jdbcTemplate.update(
        """
        INSERT INTO comments (
          post_id,
          author_id,
          content,
          status,
          created_at,
          updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        postId,
        authorId,
        content,
        status,
        createdAt,
        createdAt);
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM comments", Long.class);
  }

  private void insertSensitiveWord(String word, String level) {
    jdbcTemplate.update(
        """
        INSERT INTO sensitive_words (
          word,
          level,
          hit_count,
          created_by,
          created_at,
          updated_at
        ) VALUES (?, ?, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        word,
        level,
        ownerId);
  }
}
