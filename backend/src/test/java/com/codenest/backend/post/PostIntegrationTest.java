package com.codenest.backend.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
      "spring.datasource.url=jdbc:h2:mem:post_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class PostIntegrationTest {
  private static final String OWNER_CLERK_ID = "clerk_post_owner";
  private static final String OTHER_CLERK_ID = "clerk_post_other";
  private static final String ADMIN_CLERK_ID = "clerk_post_admin";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private PostReactionMapper postReactionMapper;

  private Long ownerId;
  private Long categoryId;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM sensitive_words");
    jdbcTemplate.update("DELETE FROM favorites");
    jdbcTemplate.update("DELETE FROM post_reactions");
    jdbcTemplate.update("DELETE FROM post_tags");
    jdbcTemplate.update("DELETE FROM posts");
    jdbcTemplate.update("DELETE FROM categories");
    jdbcTemplate.update("DELETE FROM users");

    ownerId = insertUser(OWNER_CLERK_ID, "postowner", "Post Owner", "user");
    insertUser(OTHER_CLERK_ID, "postother", "Post Other", "user");
    insertUser(ADMIN_CLERK_ID, "postadmin", "Post Admin", "admin");
    categoryId = insertCategory("Backend", "backend");
  }

  @Test
  void publicListReturnsOnlyPublishedPostsAndPaginates() throws Exception {
    insertPost(ownerId, categoryId, "Draft post", "draft", 0, 0, 0);
    insertPost(ownerId, categoryId, "First published", "published", 1, 1, 0);
    insertPost(ownerId, categoryId, "Second published", "published", 7, 2, 3);

    mockMvc
        .perform(get("/posts").param("page", "1").param("pageSize", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(1))
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].title").value("Second published"))
        .andExpect(jsonPath("$.data.items[0].id").isString())
        .andExpect(jsonPath("$.data.items[0].status").value("published"));
  }

  @Test
  void postDetailReturnsNestedAuthorCategoryAndTagsForPublishedPost() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Tagged post", "published", 0, 0, 0);
    insertTag(postId, "java");
    insertTag(postId, "spring");

    mockMvc
        .perform(get("/posts/{id}", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(String.valueOf(postId)))
        .andExpect(jsonPath("$.data.author.username").value("postowner"))
        .andExpect(jsonPath("$.data.category.slug").value("backend"))
        .andExpect(jsonPath("$.data.tags[0]").value("java"))
        .andExpect(jsonPath("$.data.tags[1]").value("spring"))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists())
        .andExpect(jsonPath("$.data.publishedAt").exists());
  }

  @Test
  void authenticatedUserCreatesUpdatesPublishesAndListsOwnPost() throws Exception {
    String createResponse =
        mockMvc
            .perform(
                post("/posts/drafts")
                    .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "Draft API",
                          "summary": "Draft summary",
                          "content": "Draft content",
                          "coverUrl": "https://example.com/cover.png",
                          "categoryId": "%s",
                          "tags": ["java", "api"],
                          "status": "draft"
                        }
                        """
                            .formatted(categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("draft"))
            .andExpect(jsonPath("$.data.tags[0]").value("java"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String postId = objectMapper.readTree(createResponse).path("data").path("id").asText();

    mockMvc
        .perform(
            put("/posts/{id}", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Updated API",
                      "summary": "Updated summary",
                      "content": "Updated content",
                      "categoryId": "%s",
                      "tags": ["spring"],
                      "status": "draft"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Updated API"))
        .andExpect(jsonPath("$.data.tags[0]").value("spring"));

    mockMvc
        .perform(
            post("/posts/{id}/publish", postId).with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("published"))
        .andExpect(jsonPath("$.data.publishedAt").exists());

    mockMvc
        .perform(get("/creator/posts").with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.items[0].title").value("Updated API"));
  }

  @Test
  void ownerCanSoftDeletePostButNonOwnerCannotModifyIt() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Owner post", "draft", 0, 0, 0);

    mockMvc
        .perform(
            put("/posts/{id}", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Stolen edit",
                      "content": "Nope",
                      "categoryId": "%s",
                      "status": "draft"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));

    mockMvc
        .perform(delete("/posts/{id}", postId).with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(delete("/posts/{id}", postId).with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    String status =
        jdbcTemplate.queryForObject("SELECT status FROM posts WHERE id = ?", String.class, postId);
    assertThat(status).isEqualTo("deleted");
  }

  @Test
  void hiddenPostCannotBeMutatedByOwner() throws Exception {
    Long updatePostId = insertPost(ownerId, categoryId, "Hidden update", "hidden", 0, 0, 0);
    Long publishPostId = insertPost(ownerId, categoryId, "Hidden publish", "hidden", 0, 0, 0);
    Long deletePostId = insertPost(ownerId, categoryId, "Hidden delete", "hidden", 0, 0, 0);

    mockMvc
        .perform(
            put("/posts/{id}", updatePostId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Owner hidden update",
                      "content": "Hidden posts should stay guarded",
                      "categoryId": "%s",
                      "status": "draft"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    mockMvc
        .perform(
            post("/posts/{id}/publish", publishPostId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    mockMvc
        .perform(
            delete("/posts/{id}", deletePostId).with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    Integer hiddenCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM posts WHERE status = 'hidden'", Integer.class);
    assertThat(hiddenCount).isEqualTo(3);
  }

  @Test
  void softDeletedPostCannotBeUpdatedOrRepublishedThroughUpdate() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Deleted post", "draft", 0, 0, 0);

    mockMvc
        .perform(delete("/posts/{id}", postId).with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put("/posts/{id}", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Restored through update",
                      "content": "This should not restore a deleted post",
                      "categoryId": "%s",
                      "status": "published"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40000));

    String status =
        jdbcTemplate.queryForObject("SELECT status FROM posts WHERE id = ?", String.class, postId);
    assertThat(status).isEqualTo("deleted");
  }

  @Test
  void tagsAreNormalizedCaseInsensitively() throws Exception {
    mockMvc
        .perform(
            post("/posts/drafts")
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Tag normalization",
                      "content": "Tags should be stable",
                      "categoryId": "%s",
                      "tags": ["Java", "java", " JAVA "],
                      "status": "draft"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tags.length()").value(1))
        .andExpect(jsonPath("$.data.tags[0]").value("java"));
  }

  @Test
  void publishedCreateWithHighSensitiveWordIsBlocked() throws Exception {
    insertSensitiveWord("launchcode", "high");

    mockMvc
        .perform(
            post("/posts/drafts")
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "launchcode announcement",
                      "content": "Safe content",
                      "categoryId": "%s",
                      "status": "published"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40022));

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM posts WHERE title LIKE '%launchcode%'", Integer.class);
    assertThat(count).isZero();
  }

  @Test
  void publishWithHighSensitiveWordIsBlocked() throws Exception {
    insertSensitiveWord("redflag", "high");
    Long postId = insertPost(ownerId, categoryId, "redflag draft", "draft", 0, 0, 0);

    mockMvc
        .perform(post("/posts/{id}/publish", postId).with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40022));

    String status =
        jdbcTemplate.queryForObject("SELECT status FROM posts WHERE id = ?", String.class, postId);
    assertThat(status).isEqualTo("draft");
  }

  @Test
  void publishedUpdateWithHighSensitiveWordIsBlocked() throws Exception {
    insertSensitiveWord("blockedupdate", "high");
    Long postId = insertPost(ownerId, categoryId, "Published post", "published", 0, 0, 0);

    mockMvc
        .perform(
            put("/posts/{id}", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OWNER_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Published post",
                      "content": "blockedupdate appears here",
                      "categoryId": "%s",
                      "status": "published"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(40022));
  }

  @Test
  void likeAndDislikeToggleAndKeepCountersCorrect() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Reactable", "published", 0, 0, 0);

    mockMvc
        .perform(post("/posts/{id}/like", postId).with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(1));

    mockMvc
        .perform(post("/posts/{id}/like", postId).with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(0));

    mockMvc
        .perform(
            post("/posts/{id}/dislike", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(0));

    mockMvc
        .perform(post("/posts/{id}/like", postId).with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(1));

    Integer dislikeCount =
        jdbcTemplate.queryForObject(
            "SELECT dislike_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(dislikeCount).isZero();
  }

  @Test
  void likeRemovesExistingDislikeAndBoundsCounter() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Existing dislike", "published", 0, 0, 0);
    insertReaction(postId, otherId(), "dislike");

    mockMvc
        .perform(post("/posts/{id}/like", postId).with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(1));

    Integer dislikeCount =
        jdbcTemplate.queryForObject(
            "SELECT dislike_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(dislikeCount).isZero();

    String reaction =
        jdbcTemplate.queryForObject(
            "SELECT reaction FROM post_reactions WHERE post_id = ? AND user_id = ?",
            String.class,
            postId,
            otherId());
    assertThat(reaction).isEqualTo("like");
  }

  @Test
  void conditionalReactionSwitchDoesNotUpdateStaleReaction() {
    Long postId = insertPost(ownerId, categoryId, "Stale reaction switch", "published", 0, 0, 0);
    Long userId = otherId();
    insertReaction(postId, userId, "like");

    int staleUpdated =
        postReactionMapper.updateReactionIfCurrent(postId, userId, "dislike", "like");
    assertThat(staleUpdated).isZero();

    String reaction =
        jdbcTemplate.queryForObject(
            "SELECT reaction FROM post_reactions WHERE post_id = ? AND user_id = ?",
            String.class,
            postId,
            userId);
    assertThat(reaction).isEqualTo("like");

    int currentUpdated =
        postReactionMapper.updateReactionIfCurrent(postId, userId, "like", "dislike");
    assertThat(currentUpdated).isEqualTo(1);
  }

  @Test
  void conditionalReactionDeleteDoesNotRemoveStaleReaction() {
    Long postId = insertPost(ownerId, categoryId, "Stale reaction delete", "published", 0, 0, 0);
    Long userId = otherId();
    insertReaction(postId, userId, "dislike");

    int staleDeleted = postReactionMapper.deleteReactionIfCurrent(postId, userId, "like");
    assertThat(staleDeleted).isZero();

    String reaction =
        jdbcTemplate.queryForObject(
            "SELECT reaction FROM post_reactions WHERE post_id = ? AND user_id = ?",
            String.class,
            postId,
            userId);
    assertThat(reaction).isEqualTo("dislike");

    int currentDeleted = postReactionMapper.deleteReactionIfCurrent(postId, userId, "dislike");
    assertThat(currentDeleted).isEqualTo(1);
  }

  @Test
  void favoriteTogglesAndKeepsCounterCorrect() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Favorite target", "published", 0, 0, 0);

    mockMvc
        .perform(
            post("/posts/{id}/favorite", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.favoriteCount").value(1));

    mockMvc
        .perform(
            post("/posts/{id}/favorite", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.favoriteCount").value(0));
  }

  @Test
  void favoriteToggleDoesNotDriveCounterNegativeWhenExistingRowCounterIsStale() throws Exception {
    Long postId = insertPost(ownerId, categoryId, "Favorite stale counter", "published", 0, 0, 0);
    insertFavorite(postId, otherId());

    mockMvc
        .perform(
            post("/posts/{id}/favorite", postId)
                .with(jwt().jwt(jwt -> jwt.subject(OTHER_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.favoriteCount").value(0));

    Integer favoriteCount =
        jdbcTemplate.queryForObject(
            "SELECT favorite_count FROM posts WHERE id = ?", Integer.class, postId);
    assertThat(favoriteCount).isZero();
  }

  private Long insertUser(String clerkUserId, String username, String displayName, String role) {
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
        ) VALUES (?, ?, ?, '', '', ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        clerkUserId,
        username,
        displayName,
        role);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM users WHERE clerk_user_id = ?", Long.class, clerkUserId);
  }

  private Long otherId() {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM users WHERE clerk_user_id = ?", Long.class, OTHER_CLERK_ID);
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
        ) VALUES (?, ?, '', 1, 'active', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        name,
        slug);
    return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE slug = ?", Long.class, slug);
  }

  private Long insertPost(
      Long authorId,
      Long categoryId,
      String title,
      String status,
      int viewCount,
      int likeCount,
      int commentCount) {
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
        ) VALUES (?, ?, ?, 'Summary', 'Content', '', ?, ?, ?, 0, 0, ?, CASE WHEN ? = 'published' THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        authorId,
        categoryId,
        title,
        status,
        viewCount,
        likeCount,
        commentCount,
        status);
    return jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
  }

  private void insertTag(Long postId, String tag) {
    jdbcTemplate.update(
        "INSERT INTO post_tags (post_id, tag, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
        postId,
        tag);
  }

  private void insertReaction(Long postId, Long userId, String reaction) {
    jdbcTemplate.update(
        """
        INSERT INTO post_reactions (
          post_id,
          user_id,
          reaction,
          created_at,
          updated_at
        ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        postId,
        userId,
        reaction);
  }

  private void insertFavorite(Long postId, Long userId) {
    jdbcTemplate.update(
        "INSERT INTO favorites (post_id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
        postId,
        userId);
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
