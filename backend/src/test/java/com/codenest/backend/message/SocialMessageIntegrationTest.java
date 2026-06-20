package com.codenest.backend.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
      "spring.datasource.url=jdbc:h2:mem:social_message_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class SocialMessageIntegrationTest {
  private static final String ALICE_CLERK_ID = "clerk_social_alice";
  private static final String BOB_CLERK_ID = "clerk_social_bob";
  private static final String CAROL_CLERK_ID = "clerk_social_carol";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  private Long aliceId;
  private Long bobId;
  private Long carolId;
  private Long categoryId;
  private Long alicePostId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM sensitive_word_hits");
    jdbcTemplate.update("DELETE FROM sensitive_words");
    jdbcTemplate.update("DELETE FROM notifications");
    jdbcTemplate.update("DELETE FROM messages");
    jdbcTemplate.update("DELETE FROM follows");
    jdbcTemplate.update("DELETE FROM comments");
    jdbcTemplate.update("DELETE FROM favorites");
    jdbcTemplate.update("DELETE FROM post_reactions");
    jdbcTemplate.update("DELETE FROM post_tags");
    jdbcTemplate.update("DELETE FROM posts");
    jdbcTemplate.update("DELETE FROM category_moderators");
    jdbcTemplate.update("DELETE FROM categories");
    jdbcTemplate.update("DELETE FROM users");

    aliceId = insertUser(ALICE_CLERK_ID, "socialalice", "Social Alice");
    bobId = insertUser(BOB_CLERK_ID, "socialbob", "Social Bob");
    carolId = insertUser(CAROL_CLERK_ID, "socialcarol", "Social Carol");
    categoryId = insertCategory();
    alicePostId = insertPost(aliceId);
  }

  @Test
  void followTogglesFollowerCountAndCreatesNotification() throws Exception {
    mockMvc
        .perform(post("/users/{id}/follow", aliceId).with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(String.valueOf(aliceId)))
        .andExpect(jsonPath("$.data.followerCount").value(1));

    Integer followRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM follows", Integer.class);
    Integer notificationRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'follow'",
            Integer.class,
            aliceId);
    assertThat(followRows).isEqualTo(1);
    assertThat(notificationRows).isEqualTo(1);

    mockMvc
        .perform(post("/users/{id}/follow", aliceId).with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.followerCount").value(0));

    Integer followerCount =
        jdbcTemplate.queryForObject(
            "SELECT follower_count FROM users WHERE id = ?", Integer.class, aliceId);
    assertThat(followerCount).isZero();
  }

  @Test
  void selfFollowIsBlocked() throws Exception {
    mockMvc
        .perform(
            post("/users/{id}/follow", aliceId).with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));
  }

  @Test
  void notificationsListAndReadAreScopedToCurrentUser() throws Exception {
    Long aliceNotificationId = insertNotification(aliceId, "Alice notice");
    Long bobNotificationId = insertNotification(bobId, "Bob notice");

    mockMvc
        .perform(get("/notifications").with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(String.valueOf(aliceNotificationId)))
        .andExpect(jsonPath("$.data[0].title").value("Test"))
        .andExpect(jsonPath("$.data[0].content").value("Alice notice"))
        .andExpect(jsonPath("$.data[0].createdAt").exists());

    mockMvc
        .perform(
            post("/notifications/{id}/read", bobNotificationId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(40004));

    mockMvc
        .perform(
            post("/notifications/{id}/read", aliceNotificationId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(String.valueOf(aliceNotificationId)))
        .andExpect(jsonPath("$.data.readAt").exists());
  }

  @Test
  void commentingNotifiesPostAuthorExceptOwnComment() throws Exception {
    mockMvc
        .perform(
            post("/posts/{postId}/comments", alicePostId)
                .with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Nice post\"}"))
        .andExpect(status().isOk());

    Integer afterOtherComment =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'comment'",
            Integer.class,
            aliceId);
    assertThat(afterOtherComment).isEqualTo(1);

    mockMvc
        .perform(
            post("/posts/{postId}/comments", alicePostId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Author note\"}"))
        .andExpect(status().isOk());

    Integer afterOwnComment =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'comment'",
            Integer.class,
            aliceId);
    assertThat(afterOwnComment).isEqualTo(1);
  }

  @Test
  void messagesCreateThreadsAndReadingMarksIncomingMessagesRead() throws Exception {
    mockMvc
        .perform(
            post("/messages/threads/{threadId}", aliceId)
                .with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" Hello Alice \"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.threadId").value(String.valueOf(aliceId)))
        .andExpect(jsonPath("$.data.sender.username").value("socialbob"))
        .andExpect(jsonPath("$.data.content").value("Hello Alice"));

    mockMvc
        .perform(
            post("/messages/threads/{threadId}", bobId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hi Bob\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/messages/threads").with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(String.valueOf(bobId)))
        .andExpect(jsonPath("$.data[0].participant.username").value("socialbob"))
        .andExpect(jsonPath("$.data[0].lastMessage.content").value("Hi Bob"))
        .andExpect(jsonPath("$.data[0].unreadCount").value(1))
        .andExpect(jsonPath("$.data[0].updatedAt").exists());

    mockMvc
        .perform(
            get("/messages/threads/{threadId}", bobId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].content").value("Hello Alice"))
        .andExpect(jsonPath("$.data[0].readAt").exists())
        .andExpect(jsonPath("$.data[1].content").value("Hi Bob"));

    Integer unread =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND read_at IS NULL",
            Integer.class,
            aliceId);
    assertThat(unread).isZero();
  }

  @Test
  void selfMessageIsBlocked() throws Exception {
    mockMvc
        .perform(
            post("/messages/threads/{threadId}", aliceId)
                .with(jwt().jwt(jwt -> jwt.subject(ALICE_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"note to self\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));
  }

  @Test
  void highSensitiveWordBlocksMessageWith40022() throws Exception {
    insertSensitiveWord("messageblock", "high");

    mockMvc
        .perform(
            post("/messages/threads/{threadId}", aliceId)
                .with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"messageblock should fail\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40022));

    Integer messages = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class);
    Integer hits =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sensitive_word_hits WHERE resource_type = 'message'",
            Integer.class);
    assertThat(messages).isZero();
    assertThat(hits).isEqualTo(1);
  }

  @Test
  void lowSensitiveWordMessageRecordsHit() throws Exception {
    insertSensitiveWord("messagewarn", "low");

    String response =
        mockMvc
            .perform(
                post("/messages/threads/{threadId}", aliceId)
                    .with(jwt().jwt(jwt -> jwt.subject(BOB_CLERK_ID)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"messagewarn is allowed\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long messageId = objectMapper.readTree(response).path("data").path("id").asLong();

    Integer hits =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sensitive_word_hits WHERE resource_type = 'message' AND resource_id = ?",
            Integer.class,
            messageId);
    assertThat(hits).isEqualTo(1);
  }

  private Long insertUser(String clerkUserId, String username, String displayName) {
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
        ) VALUES (?, ?, ?, '', '', 'user', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        clerkUserId,
        username,
        displayName);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM users WHERE clerk_user_id = ?", Long.class, clerkUserId);
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
        ) VALUES ('Social', 'social', '', 1, 'active', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE slug = 'social'", Long.class);
  }

  private Long insertPost(Long authorId) {
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
        ) VALUES (?, ?, 'Social post', 'Summary', 'Content', '', 'published', 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        authorId,
        categoryId);
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
  }

  private Long insertNotification(Long userId, String content) {
    jdbcTemplate.update(
        """
        INSERT INTO notifications (
          user_id,
          type,
          title,
          content,
          created_at
        ) VALUES (?, 'test', 'Test', ?, CURRENT_TIMESTAMP)
        """,
        userId,
        content);
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM notifications", Long.class);
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
        aliceId);
  }
}
