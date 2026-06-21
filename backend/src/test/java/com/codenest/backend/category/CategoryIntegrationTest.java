package com.codenest.backend.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
      "spring.datasource.url=jdbc:h2:mem:category_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class CategoryIntegrationTest {
  private static final String ADMIN_CLERK_ID = "clerk_category_admin";
  private static final String USER_CLERK_ID = "clerk_category_user";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM categories");
    jdbcTemplate.update("DELETE FROM users");
    insertUser(ADMIN_CLERK_ID, "categoryadmin", "Category Admin", "admin");
    insertUser(USER_CLERK_ID, "categoryuser", "Category User", "user");
  }

  @Test
  void publicCategoriesReturnOnlyActiveCategoriesSortedAndUnauthenticated() throws Exception {
    insertCategory("Disabled", "disabled", "Hidden", 0, "disabled", 0);
    insertCategory("Second", "second", "Second active", 20, "active", 2);
    insertCategory("First B", "first-b", "First active B", 10, "active", 1);
    insertCategory("First A", "first-a", "First active A", 10, "active", 3);

    mockMvc
        .perform(get("/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].slug").value("first-b"))
        .andExpect(jsonPath("$.data[0].id").isString())
        .andExpect(jsonPath("$.data[0].name").value("First B"))
        .andExpect(jsonPath("$.data[0].description").value("First active B"))
        .andExpect(jsonPath("$.data[0].postCount").value(1))
        .andExpect(jsonPath("$.data[1].slug").value("first-a"))
        .andExpect(jsonPath("$.data[2].slug").value("second"));
  }

  @Test
  void normalAuthenticatedUserCannotAccessAdminCategories() throws Exception {
    mockMvc
        .perform(get("/admin/categories").with(jwt().jwt(jwt -> jwt.subject(USER_CLERK_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));
  }

  @Test
  void adminCanCreateCategory() throws Exception {
    mockMvc
        .perform(
            post("/admin/categories")
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Java",
                      "slug": "java",
                      "description": "Java discussions",
                      "sortOrder": 5
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").isString())
        .andExpect(jsonPath("$.data.name").value("Java"))
        .andExpect(jsonPath("$.data.slug").value("java"))
        .andExpect(jsonPath("$.data.description").value("Java discussions"))
        .andExpect(jsonPath("$.data.postCount").value(0));

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM categories WHERE slug = ? AND status = 'active'",
            Integer.class,
            "java");
    assertThat(count).isEqualTo(1);
  }

  @Test
  void duplicateSlugReturnsDuplicateAppCode() throws Exception {
    insertCategory("Existing Java", "java", "Existing", 1, "active", 0);

    mockMvc
        .perform(
            post("/admin/categories")
                .with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Java Duplicate",
                      "slug": "java",
                      "description": "Duplicate"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(40009));
  }

  @Test
  void deleteEmptyCategoryHardDeletes() throws Exception {
    Long id = insertCategory("Empty", "empty", "Empty", 1, "active", 0);

    mockMvc
        .perform(
            delete("/admin/categories/{id}", id).with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM categories WHERE id = ?", Integer.class, id);
    assertThat(count).isZero();
  }

  @Test
  void deleteCategoryWithPostsDisablesIt() throws Exception {
    Long id = insertCategory("Non Empty", "non-empty", "Has posts", 1, "active", 3);

    mockMvc
        .perform(
            delete("/admin/categories/{id}", id).with(jwt().jwt(jwt -> jwt.subject(ADMIN_CLERK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    String status =
        jdbcTemplate.queryForObject("SELECT status FROM categories WHERE id = ?", String.class, id);
    assertThat(status).isEqualTo("disabled");
  }

  private void insertUser(String clerkUserId, String username, String displayName, String role) {
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
  }

  private Long insertCategory(
      String name, String slug, String description, int sortOrder, String status, int postCount) {
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
        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        name,
        slug,
        description,
        sortOrder,
        status,
        postCount);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM categories WHERE slug = ?", Long.class, slug);
  }
}
