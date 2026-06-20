# CodeNest Backend V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot 3 backend for CodeNest with Clerk JWT auth, MyBatis-Plus data access, MySQL 8 storage, S3-compatible image uploads, public content APIs, creator APIs, and admin APIs.

**Architecture:** Create a standalone `backend/` Spring Boot service that exposes `/api/**` endpoints matching the existing Vue service contracts. Spring Security validates Clerk JWTs, an application user context maps Clerk identities to local `users` rows, services enforce owner/moderator/admin permissions, and MyBatis-Plus mappers persist data in MySQL.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security OAuth2 Resource Server, MyBatis-Plus, MySQL 8, Flyway, springdoc-openapi, AWS SDK v2 S3 client, JUnit 5, Testcontainers, Maven.

---

## Spec Source

Implement from:

- `docs/superpowers/specs/2026-06-20-codenest-backend-api-design.md`

## File Structure To Create

```text
backend/
  pom.xml
  src/main/java/com/codenest/backend/
    CodeNestBackendApplication.java
    common/
      ApiResponse.java
      PageResult.java
      ErrorCode.java
      BusinessException.java
      GlobalExceptionHandler.java
    config/
      SecurityConfig.java
      MybatisPlusConfig.java
      OpenApiConfig.java
      StorageProperties.java
      S3StorageConfig.java
    security/
      CurrentUser.java
      CurrentUserProvider.java
      ClerkUserSyncService.java
      PermissionService.java
    user/
      UserEntity.java
      UserMapper.java
      UserService.java
      UserController.java
      dto/UserDto.java
    category/
      CategoryEntity.java
      CategoryModeratorEntity.java
      CategoryMapper.java
      CategoryModeratorMapper.java
      CategoryService.java
      CategoryController.java
      dto/CategoryDto.java
    post/
      PostEntity.java
      PostTagEntity.java
      PostReactionEntity.java
      FavoriteEntity.java
      PostMapper.java
      PostTagMapper.java
      PostReactionMapper.java
      FavoriteMapper.java
      PostService.java
      PostController.java
      dto/PostDto.java
      dto/PostDraftRequest.java
      dto/PostQuery.java
    comment/
      CommentEntity.java
      CommentMapper.java
      CommentService.java
      CommentController.java
      dto/CommentDto.java
    social/
      FollowEntity.java
      FollowMapper.java
      FollowService.java
    message/
      MessageEntity.java
      NotificationEntity.java
      MessageMapper.java
      NotificationMapper.java
      MessageService.java
      NotificationService.java
      MessageController.java
      NotificationController.java
      dto/MessageDto.java
      dto/MessageThreadDto.java
      dto/NotificationDto.java
    moderation/
      SensitiveWordEntity.java
      SensitiveWordHitEntity.java
      SensitiveWordMapper.java
      SensitiveWordHitMapper.java
      SensitiveWordService.java
    admin/
      AdminController.java
      AdminService.java
      AuditLogEntity.java
      AuditLogMapper.java
      dto/AdminMetricDto.java
      dto/AdminModeratorSectionDto.java
      dto/SensitiveWordDto.java
    upload/
      FileObjectEntity.java
      FileObjectMapper.java
      StorageService.java
      UploadController.java
      dto/UploadResultDto.java
  src/main/resources/
    application.yml
    application-dev.yml
    db/migration/
      V1__init_schema.sql
    db/dev-migration/
      V2__seed_dev_data.sql
  src/test/java/com/codenest/backend/
    auth/AuthIntegrationTest.java
    post/PostServiceTest.java
    admin/AdminAuthorizationTest.java
    moderation/SensitiveWordServiceTest.java
```

## Implementation Tasks

### Task 1: Scaffold Spring Boot Backend

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/codenest/backend/CodeNestBackendApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: Create Maven project directories**

Run:

```bash
mkdir -p backend/src/main/java/com/codenest/backend backend/src/main/resources backend/src/test/java/com/codenest/backend
```

Expected: backend source directories exist.

- [ ] **Step 2: Create `backend/pom.xml`**

Use Java 21 and Spring Boot 3 with these dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
  </dependency>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.66</version>
  </dependency>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

- [ ] **Step 3: Create application entry point**

Create `CodeNestBackendApplication.java`:

```java
package com.codenest.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeNestBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(CodeNestBackendApplication.class, args);
  }
}
```

- [ ] **Step 4: Create base config**

Create `application.yml` with `/api` context path and profile-driven values:

```yaml
server:
  servlet:
    context-path: /api

spring:
  application:
    name: codenest-backend
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:codenest}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:codenest}
    password: ${MYSQL_PASSWORD:codenest}
  flyway:
    enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${CLERK_ISSUER}
          jwk-set-uri: ${CLERK_JWKS_URI}

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    banner: false

codenest:
  storage:
    endpoint: ${S3_ENDPOINT}
    region: ${S3_REGION:auto}
    bucket: ${S3_BUCKET:codenest}
    access-key: ${S3_ACCESS_KEY}
    secret-key: ${S3_SECRET_KEY}
    public-base-url: ${S3_PUBLIC_BASE_URL}
```

- [ ] **Step 5: Verify scaffold**

Run:

```bash
cd backend && mvn test
```

Expected: build succeeds with zero tests or generated context checks.

### Task 2: Add Common Response And Error Handling

**Files:**
- Create: `backend/src/main/java/com/codenest/backend/common/ApiResponse.java`
- Create: `backend/src/main/java/com/codenest/backend/common/PageResult.java`
- Create: `backend/src/main/java/com/codenest/backend/common/ErrorCode.java`
- Create: `backend/src/main/java/com/codenest/backend/common/BusinessException.java`
- Create: `backend/src/main/java/com/codenest/backend/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Add response classes**

Create records matching frontend `unwrap`:

```java
package com.codenest.backend.common;

public record ApiResponse<T>(int code, String message, T data) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "ok", data);
  }

  public static <T> ApiResponse<T> error(ErrorCode code, String message) {
    return new ApiResponse<>(code.code(), message, null);
  }
}
```

```java
package com.codenest.backend.common;

import java.util.List;

public record PageResult<T>(List<T> items, long total, long page, long pageSize) {}
```

- [ ] **Step 2: Add error model**

Create:

```java
package com.codenest.backend.common;

public enum ErrorCode {
  BAD_REQUEST(40000),
  UNAUTHORIZED(40001),
  FORBIDDEN(40003),
  NOT_FOUND(40004),
  DUPLICATE(40009),
  SENSITIVE_WORD_BLOCKED(40022),
  SERVER_ERROR(50000);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }
}
```

```java
package com.codenest.backend.common;

public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
```

- [ ] **Step 3: Add global handler**

Map validation, business, and unexpected errors to `ApiResponse.error(...)`.

- [ ] **Step 4: Test response contract**

Create a small MVC test that calls a test-only controller or exception handler directly and asserts:

```java
assertThat(response.getCode()).isEqualTo(40000);
assertThat(response.getData()).isNull();
```

### Task 3: Create Database Migrations

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/dev-migration/V2__seed_dev_data.sql`

- [ ] **Step 1: Create all V1 tables**

Use the table definitions from `docs/superpowers/specs/2026-06-20-codenest-backend-api-design.md` and create these tables:

```text
users, categories, category_moderators, posts, post_tags, post_reactions,
favorites, follows, comments, messages, notifications, sensitive_words,
sensitive_word_hits, audit_logs, file_objects
```

- [ ] **Step 2: Add required unique indexes**

Ensure these constraints exist:

```sql
unique key uk_users_clerk_user_id (clerk_user_id),
unique key uk_users_username (username),
unique key uk_categories_slug (slug),
unique key uk_category_moderators (category_id, user_id),
unique key uk_post_tags (post_id, tag),
unique key uk_post_reactions (post_id, user_id),
unique key uk_favorites (post_id, user_id),
unique key uk_follows (follower_id, following_id),
unique key uk_sensitive_words_word (word),
unique key uk_file_objects_bucket_key (bucket, object_key)
```

- [ ] **Step 3: Seed dev data**

Insert one admin, one normal user, three categories, and a small sensitive-word set in the dev-only Flyway location. Use stable usernames:

```text
admin, writer
```

- [ ] **Step 4: Verify migration**

Run:

```bash
cd backend && mvn test
```

Expected: Flyway migration succeeds against the test database.

### Task 4: Configure Security And Clerk User Sync

**Files:**
- Create: `backend/src/main/java/com/codenest/backend/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/codenest/backend/security/CurrentUser.java`
- Create: `backend/src/main/java/com/codenest/backend/security/CurrentUserProvider.java`
- Create: `backend/src/main/java/com/codenest/backend/security/ClerkUserSyncService.java`
- Create: `backend/src/test/java/com/codenest/backend/auth/AuthIntegrationTest.java`

- [ ] **Step 1: Configure route security**

Rules:

```text
GET /posts/**, GET /categories, GET /users/** are public.
/auth/me, /auth/sync, /creator/**, /messages/**, /notifications/**, /uploads/** require authentication.
/admin/** requires authentication and is checked by PermissionService for admin/moderator scope.
```

- [ ] **Step 2: Map Clerk claims to local user**

Read JWT claims:

```text
sub, username, name, image_url
```

When local user is missing, create:

```text
role=user, status=active, bio='', counters=0
```

- [ ] **Step 3: Add `/auth/me`**

Return the existing frontend `User` shape:

```json
{
  "id": "1",
  "username": "writer",
  "displayName": "Writer",
  "avatarUrl": "",
  "bio": "",
  "role": "user",
  "status": "active",
  "muteUntil": null,
  "postCount": 0,
  "likeCount": 0,
  "favoriteCount": 0,
  "followerCount": 0
}
```

- [ ] **Step 4: Test authenticated user sync**

Use `spring-security-test` JWT support to call `/auth/me` with `sub=clerk_user_1`.

Expected: response code is `0` and `users.clerk_user_id = clerk_user_1` exists.

### Task 5: Implement Category APIs

**Files:**
- Create category entity, mapper, service, controller, DTOs.
- Add admin category mutations.

- [ ] **Step 1: Implement public list**

`GET /categories` returns active categories sorted by `sort_order`, then `id`.

- [ ] **Step 2: Implement admin list**

`GET /admin/categories` returns all categories with `postCount`.

- [ ] **Step 3: Implement admin create/update/delete**

Validation:

```text
name: 1-80 chars
slug: lowercase letters, numbers, hyphen, 1-100 chars
description: max 500 chars
```

Delete behavior: set `status=disabled` when category has posts; hard delete only when empty.

- [ ] **Step 4: Test slug uniqueness**

Creating duplicate slug returns `40009`.

### Task 6: Implement Post And Interaction APIs

**Files:**
- Create post entities, mappers, service, controller, DTOs.
- Create reaction and favorite logic.

- [ ] **Step 1: Implement post query**

Support params:

```text
keyword, categoryId, categorySlug, authorId, tags, status, page, pageSize, sortBy
```

Public `/posts` only returns `published`.

- [ ] **Step 2: Implement post detail**

`GET /posts/{id}` returns `PostDto` with nested `author`, `category`, and `tags`.

- [ ] **Step 3: Implement draft/create/update/publish/delete**

Owner can mutate own post. `status=published` creation must run sensitive-word detection before saving as published.

- [ ] **Step 4: Implement like/dislike/favorite toggles**

Rules:

```text
like toggles like and removes existing dislike.
dislike toggles dislike and removes existing like.
favorite toggles favorite.
counters are updated in the same transaction.
```

- [ ] **Step 5: Test post lifecycle**

Test draft create, update, publish, query as public, favorite toggle, delete.

### Task 7: Implement Comments And Sensitive Word Blocking

**Files:**
- Create comment entity, mapper, service, controller, DTOs.
- Create sensitive-word entities, mapper, service.
- Create: `backend/src/test/java/com/codenest/backend/moderation/SensitiveWordServiceTest.java`

- [ ] **Step 1: Implement sensitive word scanner**

Load active words from DB and classify hits by max level:

```text
none -> allow
low -> allow and record
medium -> allow and record
high -> throw BusinessException(SENSITIVE_WORD_BLOCKED)
```

- [ ] **Step 2: Implement comment list**

`GET /posts/{postId}/comments` returns visible comments sorted by `created_at asc`.

- [ ] **Step 3: Implement create comment**

Reject banned users and muted users where `mute_until > now()`.

- [ ] **Step 4: Implement delete comment**

Allow owner, category moderator, and admin. Use soft delete status.

- [ ] **Step 5: Test high-risk blocking**

Seed a high-risk sensitive word, post a comment containing it, and assert response code `40022`.

### Task 8: Implement Follow, Notification, And Message APIs

**Files:**
- Create social, notification, and message entities/mappers/services/controllers.

- [ ] **Step 1: Implement follow toggle**

`POST /users/{id}/follow` toggles relation and updates target user's `follower_count`.

- [ ] **Step 2: Create notifications for key events**

Create notification rows for:

```text
new comment on user's post
new follower
post hidden by moderator/admin
```

- [ ] **Step 3: Implement notification list/read**

Return current user's notifications sorted by `created_at desc`.

- [ ] **Step 4: Implement message polling**

For `/messages/threads`, group messages by the other participant and return last message, unread count, and updated time.

- [ ] **Step 5: Implement send message**

Use `threadId` as the other participant user id. Save sender, receiver, and content after sensitive-word detection.

### Task 9: Implement Uploads

**Files:**
- Create: `backend/src/main/java/com/codenest/backend/config/StorageProperties.java`
- Create: `backend/src/main/java/com/codenest/backend/config/S3StorageConfig.java`
- Create upload package files.

- [ ] **Step 1: Bind storage config**

Properties:

```text
endpoint, region, bucket, accessKey, secretKey, publicBaseUrl
```

- [ ] **Step 2: Implement image validation**

Accept:

```text
image/png, image/jpeg, image/webp, image/gif
```

Maximum size:

```text
5 MB
```

- [ ] **Step 3: Upload object**

Object key format:

```text
uploads/{yyyy}/{MM}/{ownerId}/{uuid}.{extension}
```

- [ ] **Step 4: Store metadata**

Insert `file_objects` row with bucket, key, URL, content type, and size.

### Task 10: Implement Admin And Moderator APIs

**Files:**
- Create admin controller/service/DTOs.
- Create audit log entity and mapper.
- Create: `backend/src/test/java/com/codenest/backend/admin/AdminAuthorizationTest.java`

- [ ] **Step 1: Implement metrics**

`GET /admin/metrics` returns:

```text
total users, published posts, pending/hidden posts, sensitive hits
```

- [ ] **Step 2: Implement user management**

`GET /admin/users` and `PATCH /admin/users/{id}/status`.

Allowed statuses:

```text
active, banned
```

Mute updates `mute_until`.

- [ ] **Step 3: Implement moderator-scoped post management**

Admins can see all posts. Moderators see posts where category is assigned to them.

- [ ] **Step 4: Implement sensitive word CRUD**

Keep `word` unique and `level` in:

```text
low, medium, high
```

- [ ] **Step 5: Implement audit logging**

Log admin and moderator mutations:

```text
user_status_update, post_status_update, category_create, category_update,
category_delete, moderator_assign, moderator_remove, sensitive_word_create,
sensitive_word_update, sensitive_word_delete
```

- [ ] **Step 6: Test moderator boundaries**

Create two categories and assign moderator to one. Assert moderator can hide posts in assigned category and receives `40003` for the other category.

### Task 11: Add OpenAPI And Frontend Integration Settings

**Files:**
- Create: `backend/src/main/java/com/codenest/backend/config/OpenApiConfig.java`
- Modify frontend `src/services/http.ts` in a later integration task.

- [ ] **Step 1: Enable OpenAPI UI**

Expose:

```text
/api/swagger-ui/index.html
/api/v3/api-docs
```

- [ ] **Step 2: Document auth scheme**

OpenAPI security scheme:

```text
type=http, scheme=bearer, bearerFormat=JWT
```

- [ ] **Step 3: Add CORS**

Allow configured frontend origins:

```text
http://localhost:5173
https://*.netlify.app
production custom domain
```

- [ ] **Step 4: Update frontend base URL**

Change frontend Axios config to:

```ts
baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api'
```

Use Clerk `getToken()` to set `Authorization` instead of reading `codenest_token`.

### Task 12: End-To-End Verification

**Files:**
- Create backend integration tests for auth, posts, admin authorization, and sensitive words.
- Update README backend section.

- [ ] **Step 1: Run backend tests**

Run:

```bash
cd backend && mvn test
```

Expected: all unit and integration tests pass.

- [ ] **Step 2: Run frontend tests**

Run:

```bash
npm run test:unit
```

Expected: existing frontend tests pass after API base URL changes.

- [ ] **Step 3: Run local manual smoke**

Start backend:

```bash
cd backend && mvn spring-boot:run
```

Start frontend:

```bash
npm run dev
```

Verify:

```text
GET /api/categories returns code 0.
GET /api/posts returns code 0.
Authenticated /api/auth/me returns current user.
Admin /api/admin/metrics returns code 0 for admin and 40003 for normal user.
High-risk sensitive word blocks comment creation with code 40022.
Image upload returns a URL.
```

- [ ] **Step 4: Commit**

```bash
git add backend src/services/http.ts README.md docs/superpowers
git commit -m "feat: add codenest backend v1"
```

## Execution Choice

Plan complete and saved to `docs/superpowers/plans/2026-06-20-codenest-backend-v1.md`.

Two execution options:

1. Subagent-Driven (recommended): dispatch a fresh subagent per task, review between tasks, fast iteration.
2. Inline Execution: execute tasks in this session using checkpoints.
