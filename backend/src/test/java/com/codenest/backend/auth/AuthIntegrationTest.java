package com.codenest.backend.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:auth_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:schema-auth-test.sql",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
class AuthIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void meSyncsAuthenticatedClerkUserAndReturnsFrontendUserShape() throws Exception {
    mockMvc
        .perform(
            get("/auth/me")
                .with(
                    jwt()
                        .jwt(
                            jwt ->
                                jwt.subject("clerk_user_1")
                                    .claim("username", "clerkpeng")
                                    .claim("name", "Peng Clerk")
                                    .claim("image_url", "https://cdn.example.com/peng.png"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.message").value("ok"))
        .andExpect(jsonPath("$.data.id").value(notNullValue()))
        .andExpect(jsonPath("$.data.username").value("clerkpeng"))
        .andExpect(jsonPath("$.data.displayName").value("Peng Clerk"))
        .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/peng.png"))
        .andExpect(jsonPath("$.data.bio").value(""))
        .andExpect(jsonPath("$.data.role").value("user"))
        .andExpect(jsonPath("$.data.status").value("active"))
        .andExpect(jsonPath("$.data.muteUntil").value(nullValue()))
        .andExpect(jsonPath("$.data.postCount").value(0))
        .andExpect(jsonPath("$.data.likeCount").value(0))
        .andExpect(jsonPath("$.data.favoriteCount").value(0))
        .andExpect(jsonPath("$.data.followerCount").value(0));

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE clerk_user_id = ?", Integer.class, "clerk_user_1");

    org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
  }

  @Test
  void meRejectsUnauthenticatedRequests() throws Exception {
    mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void syncReturnsCurrentUserAfterExplicitJwtSync() throws Exception {
    mockMvc
        .perform(
            post("/auth/sync")
                .with(
                    jwt()
                        .jwt(
                            jwt ->
                                jwt.subject("clerk_user_2")
                                    .claim("preferred_username", "syncpeng")
                                    .claim("given_name", "Sync")
                                    .claim("family_name", "Peng")
                                    .claim("picture", "https://cdn.example.com/sync.png"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.username").value("syncpeng"))
        .andExpect(jsonPath("$.data.displayName").value("Sync Peng"))
        .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/sync.png"));
  }

  @Test
  void adminRoutesRejectUnauthenticatedRequests() throws Exception {
    mockMvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminRoutesRejectAuthenticatedNonAdminUsers() throws Exception {
    mockMvc
        .perform(
            get("/admin/users")
                .with(jwt().jwt(jwt -> jwt.subject("clerk_user_member").claim("username", "member"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(40003));
  }

  @Test
  void adminRoutesAllowLocalAdminPastSecurityLayer() throws Exception {
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
        ) VALUES (?, ?, ?, '', '', 'admin', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        "clerk_user_admin",
        "localadmin",
        "Local Admin");

    mockMvc
        .perform(
            get("/admin/users")
                .with(
                    jwt()
                        .jwt(
                            jwt ->
                                jwt.subject("clerk_user_admin")
                                    .claim("username", "localadmin")
                                    .claim("name", "Local Admin"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }
}
