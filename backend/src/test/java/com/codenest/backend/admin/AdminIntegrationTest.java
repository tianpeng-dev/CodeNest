package com.codenest.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
      "spring.datasource.url=jdbc:h2:mem:admin_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class AdminIntegrationTest {
  private static final String ADMIN_CLERK_ID = "clerk_admin_admin";
  private static final String USER_CLERK_ID = "clerk_admin_user";
  private static final String MODERATOR_CLERK_ID = "clerk_admin_moderator";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private Long adminId;
  private Long userId;
  private Long moderatorId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM audit_logs");
    jdbcTemplate.update("DELETE FROM notifications");
    jdbcTemplate.update("DELETE FROM sensitive_word_hits");
    jdbcTemplate.update("DELETE FROM sensitive_words");
    jdbcTemplate.update("DELETE FROM category_moderators");
    jdbcTemplate.update("DELETE FROM post_tags");
    jdbcTemplate.update("DELETE FROM posts");
    jdbcTemplate.update("DELETE FROM categories");
    jdbcTemplate.update("DELETE FROM users");

    adminId = insertUser(ADMIN_CLERK_ID, "adminapi", "Admin API", "admin", "active");
    userId = insertUser(USER_CLERK_ID, "memberapi", "Member API", "user", "active");
    moderatorId =
        insertUser(MODERATOR_CLERK_ID, "modapi", "Moderator API", "moderator", "active");
  }

  @Test
  void adminMetricsReturnExpectedCounts() throws Exception {
    Long categoryId = insertCategory("Java", "java");
    insertPost(userId, categoryId, "Published", "published");
    insertPost(userId, categoryId, "Hidden", "hidden");
    Long wordId = insertSensitiveWord("watch", "low");
    insertSensitiveHit(wordId, userId, "low");
    insertSensitiveHit(wordId, userId, "low");

    mockMvc
        .perform(get("/admin/metrics").with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].label").value("Total users"))
        .andExpect(jsonPath("$.data[0].value").value(3))
        .andExpect(jsonPath("$.data[1].label").value("Published posts"))
        .andExpect(jsonPath("$.data[1].value").value(1))
        .andExpect(jsonPath("$.data[2].label").value("Hidden posts"))
        .andExpect(jsonPath("$.data[2].value").value(1))
        .andExpect(jsonPath("$.data[3].label").value("Sensitive hits"))
        .andExpect(jsonPath("$.data[3].value").value(2));
  }

  @Test
  void normalUserCannotAccessAdminEndpoints() throws Exception {
    mockMvc
        .perform(get("/admin/metrics").with(jwt().jwt(jwt -> jwt.subject(USER_CLERK_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));
  }

  @Test
  void moderatorSeesAndCanHideOnlyAssignedCategoryPosts() throws Exception {
    Long assignedCategoryId = insertCategory("Assigned", "assigned");
    Long otherCategoryId = insertCategory("Other", "other");
    insertModerator(assignedCategoryId, moderatorId);
    Long assignedPostId = insertPost(userId, assignedCategoryId, "Assigned post", "published");
    Long otherPostId = insertPost(userId, otherCategoryId, "Other post", "published");

    mockMvc
        .perform(get("/admin/posts").with(jwt().jwt(jwt -> jwt.subject(MODERATOR_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(String.valueOf(assignedPostId)));

    mockMvc
        .perform(
            patch("/admin/posts/{id}/status", assignedPostId)
                .with(jwt().jwt(jwt -> jwt.subject(MODERATOR_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"hidden","reason":"Off topic"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("hidden"));

    mockMvc
        .perform(
            patch("/admin/posts/{id}/status", otherPostId)
                .with(jwt().jwt(jwt -> jwt.subject(MODERATOR_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"hidden","reason":"Off topic"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));

    assertThat(countAuditRows("post.status.update", "post", assignedPostId)).isEqualTo(1);
    Integer notifications =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'moderation'",
            Integer.class,
            userId);
    assertThat(notifications).isEqualTo(1);
  }

  @Test
  void adminCanUpdateUserStatusAndMute() throws Exception {
    mockMvc
        .perform(
            patch("/admin/users/{id}/status", userId)
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"banned","muteUntil":"2027-01-01T00:00:00"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("banned"))
        .andExpect(jsonPath("$.data.muteUntil").value("2027-01-01T00:00:00"));

    String status =
        jdbcTemplate.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId);
    assertThat(status).isEqualTo("banned");
    assertThat(countAuditRows("user.status.update", "user", userId)).isEqualTo(1);
  }

  @Test
  void adminCanAssignAndRemoveModeratorWithRoleChanges() throws Exception {
    Long categoryId = insertCategory("Moderated", "moderated");
    Long plainUserId = insertUser("clerk_plain_mod", "plainmod", "Plain Mod", "user", "active");

    mockMvc
        .perform(
            post("/admin/moderators")
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":%d,"userId":%d}
                    """
                        .formatted(categoryId, plainUserId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(String.valueOf(categoryId)))
        .andExpect(jsonPath("$.data[0].moderatorCount").value(1));

    assertThat(roleOf(plainUserId)).isEqualTo("moderator");
    Long assignmentId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM category_moderators WHERE category_id = ? AND user_id = ?",
            Long.class,
            categoryId,
            plainUserId);
    assertThat(countAuditRows("moderator.assign", "category_moderator", categoryId)).isEqualTo(1);

    mockMvc
        .perform(
            delete("/admin/moderators/{id}", assignmentId)
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    assertThat(roleOf(plainUserId)).isEqualTo("user");
    assertThat(countAuditRows("moderator.remove", "category_moderator", assignmentId)).isEqualTo(1);
  }

  @Test
  void sensitiveWordCrudEnforcesDuplicateAppCodeAndWritesAudit() throws Exception {
    mockMvc
        .perform(
            post("/admin/sensitive-words")
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"word":"Spoiler","level":"medium"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.word").value("spoiler"))
        .andExpect(jsonPath("$.data.level").value("medium"));

    mockMvc
        .perform(
            post("/admin/sensitive-words")
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"word":"spoiler","level":"low"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(40009));

    Long id =
        jdbcTemplate.queryForObject(
            "SELECT id FROM sensitive_words WHERE word = 'spoiler'", Long.class);

    mockMvc
        .perform(
            put("/admin/sensitive-words/{id}", id)
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"word":"spoiler-updated","level":"high"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.word").value("spoiler-updated"))
        .andExpect(jsonPath("$.data.level").value("high"));

    mockMvc
        .perform(
            delete("/admin/sensitive-words/{id}", id)
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    assertThat(countAuditRows("sensitive_word.create", "sensitive_word", id)).isEqualTo(1);
    assertThat(countAuditRows("sensitive_word.update", "sensitive_word", id)).isEqualTo(1);
    assertThat(countAuditRows("sensitive_word.delete", "sensitive_word", id)).isEqualTo(1);
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

  private Long insertCategory(String name, String slug) {
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
        ) VALUES (?, ?, ?, 0, 'active', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        name,
        slug,
        name + " description");
    return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE slug = ?", Long.class, slug);
  }

  private Long insertPost(Long authorId, Long categoryId, String title, String status) {
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
          published_at,
          created_at,
          updated_at
        ) VALUES (?, ?, ?, '', 'content', '', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        authorId,
        categoryId,
        title,
        status);
    return jdbcTemplate.queryForObject("SELECT id FROM posts WHERE title = ?", Long.class, title);
  }

  private void insertModerator(Long categoryId, Long userId) {
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
        adminId);
  }

  private Long insertSensitiveWord(String word, String level) {
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
        adminId);
    return jdbcTemplate.queryForObject("SELECT id FROM sensitive_words WHERE word = ?", Long.class, word);
  }

  private void insertSensitiveHit(Long wordId, Long userId, String level) {
    jdbcTemplate.update(
        """
        INSERT INTO sensitive_word_hits (
          word_id,
          resource_type,
          resource_id,
          user_id,
          level,
          snippet,
          created_at
        ) VALUES (?, 'post', 1, ?, ?, '', CURRENT_TIMESTAMP)
        """,
        wordId,
        userId,
        level);
  }

  private long countAuditRows(String action, String resourceType, Long resourceId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM audit_logs WHERE action = ? AND resource_type = ? AND resource_id = ?",
        Long.class,
        action,
        resourceType,
        resourceId);
  }

  private String roleOf(Long userId) {
    return jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId);
  }
}
