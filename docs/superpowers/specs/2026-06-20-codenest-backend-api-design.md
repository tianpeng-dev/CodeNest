# CodeNest Backend API And Database Design

Date: 2026-06-20

## Goal

Build a Spring Boot 3 backend for CodeNest that replaces the current mock API, supports Clerk-based login, powers the Vue 3 frontend through stable `/api` contracts, and provides admin workflows for categories, posts, users, section moderators, sensitive words, and analytics.

## Confirmed Decisions

- Auth: the Vue frontend uses Clerk login; the Spring Boot backend validates Clerk JWTs from `Authorization: Bearer <token>`.
- User sync: the backend creates or updates the local `users` row on first authenticated request. Clerk webhook support is not required for V1.
- Roles: use `user`, `moderator`, and `admin`. Section permissions are stored in `category_moderators`.
- Publishing: users can publish directly. Sensitive-word detection runs before publish; high-risk hits block publish.
- Sensitive words: low and medium hits are recorded and can be returned as warnings; high hits block publish or comments.
- Moderator scope: section moderators can manage posts and comments only in categories they moderate.
- Messaging: V1 uses normal HTTP polling for notifications and private messages; no WebSocket.
- Deployment: Spring Boot is deployed independently. Netlify frontend uses an environment variable such as `VITE_API_BASE_URL`.
- Uploads: backend writes files to S3-compatible object storage and stores public/object URLs.

## External Auth Notes

Use Clerk manual JWT verification or Clerk Java backend SDK behavior as the auth basis. Clerk documents JWKS verification options including a JWKS endpoint derived from the Frontend API URL plus `/.well-known/jwks.json`, the Backend API JWKS endpoint, or a dashboard public key. For Spring Boot, the practical V1 path is Spring Security OAuth2 Resource Server with issuer/JWKS configuration, plus local user synchronization after token validation.

References:

- Clerk manual JWT verification: https://clerk.com/docs/guides/sessions/manual-jwt-verification
- Clerk Java backend SDK examples: https://github.com/clerk/clerk-sdk-java

## API Conventions

Base path:

```text
/api
```

Success response:

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

Error response:

```json
{
  "code": 40001,
  "message": "Unauthorized",
  "data": null
}
```

Pagination response:

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "pageSize": 20
}
```

Recommended error codes:

| Code | Meaning |
| --- | --- |
| `0` | Success |
| `40000` | Bad request or validation error |
| `40001` | Unauthenticated |
| `40003` | Forbidden |
| `40004` | Resource not found |
| `40009` | Duplicate resource |
| `40022` | Sensitive-word policy violation |
| `50000` | Server error |

## Domain Model

### Roles And Permissions

| Role | Capability |
| --- | --- |
| `user` | Read public content, publish posts, comment, like, favorite, follow, send private messages |
| `moderator` | User capabilities plus manage posts and comments in assigned categories |
| `admin` | Full platform management |

Authorization rules:

- A user can update or delete their own draft/published post unless the post is hidden or deleted by admin.
- A user can delete their own comment.
- A moderator can hide posts and delete comments only inside categories where `category_moderators.user_id = currentUser.id`.
- An admin can manage all users, posts, comments, categories, moderators, and sensitive words.

### Post Status

| Status | Meaning |
| --- | --- |
| `draft` | Only author and admins can view |
| `published` | Publicly visible |
| `hidden` | Hidden by admin or moderator |
| `deleted` | Soft deleted |

### Sensitive Word Levels

| Level | Behavior |
| --- | --- |
| `low` | Save hit record; allow submit |
| `medium` | Save hit record; allow submit and expose warning in admin detail |
| `high` | Block publish/comment and return `40022` |

## Database Design

All tables should use `utf8mb4`, `InnoDB`, `created_at`, `updated_at`, and soft deletion where user content is involved.

### `users`

Local user profile and role data synced from Clerk.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Snowflake or auto increment |
| `clerk_user_id` | varchar(128) unique not null | Clerk `sub` / user id |
| `username` | varchar(64) unique not null | Display handle |
| `display_name` | varchar(80) not null | Public name |
| `avatar_url` | varchar(512) not null default '' | Public avatar |
| `bio` | varchar(500) not null default '' | Profile bio |
| `role` | varchar(20) not null | `user`, `moderator`, `admin` |
| `status` | varchar(20) not null | `active`, `banned` |
| `mute_until` | datetime null | Comment/post restriction end time |
| `post_count` | int not null default 0 | Denormalized counter |
| `like_count` | int not null default 0 | Received likes |
| `favorite_count` | int not null default 0 | Received favorites |
| `follower_count` | int not null default 0 | Denormalized counter |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `uk_users_clerk_user_id(clerk_user_id)`
- `uk_users_username(username)`
- `idx_users_role_status(role, status)`

### `categories`

Content categories and moderator sections.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Category id |
| `name` | varchar(80) unique not null | Category name |
| `slug` | varchar(100) unique not null | URL slug |
| `description` | varchar(500) not null default '' | Description |
| `cover_url` | varchar(512) not null default '' | Optional visual |
| `sort_order` | int not null default 0 | Admin ordering |
| `status` | varchar(20) not null default 'active' | `active`, `disabled` |
| `post_count` | int not null default 0 | Denormalized counter |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `uk_categories_slug(slug)`
- `idx_categories_status_sort(status, sort_order)`

### `category_moderators`

Section moderator assignment.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Assignment id |
| `category_id` | bigint unsigned not null | Category id |
| `user_id` | bigint unsigned not null | Moderator user id |
| `assigned_by` | bigint unsigned not null | Admin user id |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `uk_category_moderators(category_id, user_id)`
- `idx_category_moderators_user(user_id)`

### `posts`

Blog and forum post content.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Post id |
| `author_id` | bigint unsigned not null | User id |
| `category_id` | bigint unsigned not null | Category id |
| `title` | varchar(160) not null | Post title |
| `summary` | varchar(500) not null default '' | Summary |
| `content` | mediumtext not null | Markdown content |
| `cover_url` | varchar(512) not null default '' | Cover image |
| `status` | varchar(20) not null | `draft`, `published`, `hidden`, `deleted` |
| `view_count` | int not null default 0 | Views |
| `like_count` | int not null default 0 | Likes |
| `dislike_count` | int not null default 0 | Dislikes |
| `favorite_count` | int not null default 0 | Favorites |
| `comment_count` | int not null default 0 | Comments |
| `published_at` | datetime null | Publish time |
| `hidden_reason` | varchar(500) null | Moderator/admin reason |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `idx_posts_status_published(status, published_at)`
- `idx_posts_author_status(author_id, status)`
- `idx_posts_category_status(category_id, status)`
- `ft_posts_title_summary_content(title, summary, content)` where MySQL fulltext is enabled.

### `post_tags`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Tag row id |
| `post_id` | bigint unsigned not null | Post id |
| `tag` | varchar(40) not null | Tag value |
| `created_at` | datetime not null | Created time |

Indexes:

- `uk_post_tags(post_id, tag)`
- `idx_post_tags_tag(tag)`

### `post_reactions`

Stores like and dislike.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Reaction id |
| `post_id` | bigint unsigned not null | Post id |
| `user_id` | bigint unsigned not null | User id |
| `reaction` | varchar(20) not null | `like`, `dislike` |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `uk_post_reactions(post_id, user_id)`
- `idx_post_reactions_user(user_id)`

### `favorites`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Favorite id |
| `post_id` | bigint unsigned not null | Post id |
| `user_id` | bigint unsigned not null | User id |
| `created_at` | datetime not null | Created time |

Indexes:

- `uk_favorites(post_id, user_id)`
- `idx_favorites_user(user_id, created_at)`

### `follows`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Follow id |
| `follower_id` | bigint unsigned not null | Current user |
| `following_id` | bigint unsigned not null | Target user |
| `created_at` | datetime not null | Created time |

Indexes:

- `uk_follows(follower_id, following_id)`
- `idx_follows_following(following_id)`

### `comments`

V1 uses flat comments. Nested comments can be added through `parent_id` later without changing post detail response shape.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Comment id |
| `post_id` | bigint unsigned not null | Post id |
| `author_id` | bigint unsigned not null | User id |
| `content` | varchar(2000) not null | Comment body |
| `status` | varchar(20) not null default 'visible' | `visible`, `hidden`, `deleted` |
| `hidden_reason` | varchar(500) null | Moderator/admin reason |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `idx_comments_post_status_created(post_id, status, created_at)`
- `idx_comments_author(author_id, created_at)`

### `messages`

Private messages with polling.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Message id |
| `sender_id` | bigint unsigned not null | Sender |
| `receiver_id` | bigint unsigned not null | Receiver |
| `content` | varchar(2000) not null | Message body |
| `read_at` | datetime null | Read time |
| `created_at` | datetime not null | Created time |

Indexes:

- `idx_messages_pair_created(sender_id, receiver_id, created_at)`
- `idx_messages_receiver_read(receiver_id, read_at)`

### `notifications`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Notification id |
| `user_id` | bigint unsigned not null | Receiver |
| `type` | varchar(40) not null | `comment`, `like`, `follow`, `system`, `moderation` |
| `title` | varchar(120) not null | Title |
| `content` | varchar(1000) not null | Content |
| `read_at` | datetime null | Read time |
| `created_at` | datetime not null | Created time |

Indexes:

- `idx_notifications_user_read_created(user_id, read_at, created_at)`

### `sensitive_words`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Word id |
| `word` | varchar(100) unique not null | Sensitive word |
| `level` | varchar(20) not null | `low`, `medium`, `high` |
| `hit_count` | int not null default 0 | Total hits |
| `created_by` | bigint unsigned not null | Admin id |
| `created_at` | datetime not null | Created time |
| `updated_at` | datetime not null | Updated time |

Indexes:

- `uk_sensitive_words_word(word)`
- `idx_sensitive_words_level(level)`

### `sensitive_word_hits`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Hit id |
| `word_id` | bigint unsigned not null | Sensitive word |
| `resource_type` | varchar(40) not null | `post`, `comment`, `message` |
| `resource_id` | bigint unsigned null | Null when blocked before creation |
| `user_id` | bigint unsigned not null | Submitter |
| `level` | varchar(20) not null | Copied level |
| `snippet` | varchar(300) not null default '' | Redacted context |
| `created_at` | datetime not null | Created time |

Indexes:

- `idx_sensitive_word_hits_resource(resource_type, resource_id)`
- `idx_sensitive_word_hits_user_created(user_id, created_at)`

### `audit_logs`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | Audit id |
| `operator_id` | bigint unsigned not null | Admin or moderator |
| `action` | varchar(80) not null | Operation key |
| `resource_type` | varchar(40) not null | Target type |
| `resource_id` | bigint unsigned not null | Target id |
| `detail_json` | json null | Before/after details |
| `created_at` | datetime not null | Created time |

Indexes:

- `idx_audit_logs_operator_created(operator_id, created_at)`
- `idx_audit_logs_resource(resource_type, resource_id)`

### `file_objects`

Object-storage metadata.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | bigint unsigned PK | File id |
| `owner_id` | bigint unsigned not null | Uploader |
| `bucket` | varchar(100) not null | Object bucket |
| `object_key` | varchar(512) not null | Object key |
| `url` | varchar(1024) not null | Public or signed URL |
| `content_type` | varchar(120) not null | MIME type |
| `size_bytes` | bigint not null | Size |
| `created_at` | datetime not null | Created time |

Indexes:

- `uk_file_objects_bucket_key(bucket, object_key)`
- `idx_file_objects_owner(owner_id, created_at)`

## API Design

### Auth

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/auth/me` | Clerk JWT | Return current local user, creating or updating from Clerk claims when missing |
| `POST` | `/auth/sync` | Clerk JWT | Explicitly sync current user profile |

`/auth/login`, `/auth/register`, and `/auth/logout` remain frontend mock-era methods. With Clerk enabled, frontend should stop calling password login/register endpoints and instead obtain Clerk session tokens.

### Public Posts

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/posts` | Optional | Search and list published posts |
| `GET` | `/posts/{id}` | Optional | Get published post detail; author can read own draft |
| `GET` | `/categories` | Optional | List active categories |
| `GET` | `/users/{id}` | Optional | Get public user profile |
| `GET` | `/posts/{postId}/comments` | Optional | List visible comments |

Post query params:

```text
keyword, categoryId, categorySlug, authorId, tags, status, page, pageSize, sortBy
```

`sortBy` values:

- `latest`
- `popular`
- `commented`

### Creator And User Actions

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/creator/posts` | User | List current user's posts |
| `GET` | `/creator/analytics` | User | Current user's content statistics |
| `GET` | `/creator/comments` | User | Comments received by current user's posts |
| `POST` | `/posts/drafts` | User | Create draft or published post according to `status` |
| `PUT` | `/posts/{id}` | Owner/admin | Update post |
| `POST` | `/posts/{id}/publish` | Owner/admin | Publish draft after sensitive-word check |
| `DELETE` | `/posts/{id}` | Owner/admin | Soft delete post |
| `POST` | `/posts/{id}/like` | User | Toggle like; removes dislike if present |
| `POST` | `/posts/{id}/dislike` | User | Toggle dislike; removes like if present |
| `POST` | `/posts/{id}/favorite` | User | Toggle favorite |
| `POST` | `/users/{id}/follow` | User | Toggle follow |
| `POST` | `/posts/{postId}/comments` | User | Create comment after sensitive-word check |
| `DELETE` | `/comments/{id}` | Owner/moderator/admin | Soft delete comment |

Post draft payload:

```json
{
  "title": "Spring Boot API Design",
  "summary": "A concise summary",
  "content": "# Markdown content",
  "coverUrl": "https://cdn.example.com/covers/1.png",
  "categoryId": "1",
  "tags": ["Spring Boot", "API"],
  "status": "published"
}
```

### Messages And Notifications

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/notifications` | User | List current user's notifications |
| `POST` | `/notifications/{id}/read` | User | Mark notification read |
| `GET` | `/messages/threads` | User | Build thread list from message pairs |
| `GET` | `/messages/threads/{threadId}` | User | List messages in a thread |
| `POST` | `/messages/threads/{threadId}` | User | Send a private message |

For V1, `threadId` can be the other participant's user id. The backend can derive the pair with the current user.

### Uploads

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/uploads/images` | User | Upload avatar, cover, or post image to object storage |

Request:

```text
multipart/form-data field: file
```

Response:

```json
{
  "id": "1001",
  "url": "https://cdn.example.com/codenest/2026/06/image.png",
  "contentType": "image/png",
  "sizeBytes": 123456
}
```

### Admin

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/admin/metrics` | Admin | Dashboard metric cards |
| `GET` | `/admin/users` | Admin | List users |
| `PATCH` | `/admin/users/{id}/status` | Admin | Ban, unban, mute, or unmute user |
| `GET` | `/admin/posts` | Admin/moderator scoped | List posts |
| `PATCH` | `/admin/posts/{id}/status` | Admin/moderator scoped | Hide, restore, or mark deleted |
| `GET` | `/admin/categories` | Admin | List categories |
| `POST` | `/admin/categories` | Admin | Create category |
| `PUT` | `/admin/categories/{id}` | Admin | Update category |
| `DELETE` | `/admin/categories/{id}` | Admin | Disable category if empty |
| `GET` | `/admin/moderators` | Admin | Group moderators by section |
| `POST` | `/admin/moderators` | Admin | Assign moderator to category |
| `DELETE` | `/admin/moderators/{id}` | Admin | Remove moderator assignment |
| `GET` | `/admin/sensitive-words` | Admin | List sensitive words |
| `POST` | `/admin/sensitive-words` | Admin | Create sensitive word |
| `PUT` | `/admin/sensitive-words/{id}` | Admin | Update sensitive word |
| `DELETE` | `/admin/sensitive-words/{id}` | Admin | Delete sensitive word |
| `GET` | `/admin/analytics` | Admin | Charts and statistics |

Admin status payload examples:

```json
{
  "status": "banned",
  "reason": "Spam"
}
```

```json
{
  "status": "hidden",
  "reason": "High-risk sensitive content"
}
```

## Frontend Integration Notes

Required frontend changes when backend is available:

- Change Axios `baseURL` from hardcoded `/api` to `import.meta.env.VITE_API_BASE_URL ?? '/api'`.
- Replace local `codenest_token` storage with Clerk `getToken()` integration.
- Remove mock login/register forms or convert them to Clerk-hosted/sign-in components.
- Keep existing response unwrap logic because backend uses the same `ApiResponse<T>` envelope.
- Keep existing TypeScript DTO names where possible: `User`, `Post`, `Category`, `Comment`, `MessageThread`, `MessageItem`, `NotificationItem`, `AdminMetric`, `SensitiveWord`, and `AdminModeratorSection`.

## Environment Variables

Backend:

```text
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=codenest
MYSQL_USERNAME=codenest
MYSQL_PASSWORD=codenest
CLERK_ISSUER=https://example.clerk.accounts.dev
CLERK_JWKS_URI=https://example.clerk.accounts.dev/.well-known/jwks.json
S3_ENDPOINT=https://s3.example.com
S3_REGION=auto
S3_BUCKET=codenest
S3_ACCESS_KEY=replace_me
S3_SECRET_KEY=replace_me
S3_PUBLIC_BASE_URL=https://cdn.example.com/codenest
```

Frontend:

```text
VITE_API_BASE_URL=https://api.example.com/api
VITE_CLERK_PUBLISHABLE_KEY=pk_test_replace_me
```

## Acceptance Criteria

- The Vue frontend can run against Spring Boot by changing `VITE_API_BASE_URL` and token injection.
- Clerk-authenticated users can load `/auth/me` and receive a local CodeNest user profile.
- Public post, category, user, and comment pages work without login where applicable.
- Logged-in users can publish posts, comment, favorite, like/dislike, follow, send messages, and read notifications.
- High-risk sensitive words block post publishing and comment creation with `40022`.
- Admins can manage users, posts, categories, moderators, and sensitive words.
- Moderators can manage only assigned categories' posts and comments.
- Images upload to S3-compatible object storage and return a URL usable by the frontend.
- Admin metrics and analytics endpoints return chart-ready data.
