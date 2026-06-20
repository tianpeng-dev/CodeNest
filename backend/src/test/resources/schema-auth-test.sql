DROP TABLE IF EXISTS category_moderators;
DROP TABLE IF EXISTS file_objects;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS sensitive_word_hits;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS post_reactions;
DROP TABLE IF EXISTS post_tags;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS sensitive_words;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  clerk_user_id VARCHAR(128) NOT NULL,
  username VARCHAR(64) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  avatar_url VARCHAR(512) NOT NULL DEFAULT '',
  bio VARCHAR(500) NOT NULL DEFAULT '',
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  mute_until TIMESTAMP NULL,
  post_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  follower_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_users_clerk_user_id UNIQUE (clerk_user_id),
  CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  cover_url VARCHAR(512) NOT NULL DEFAULT '',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  post_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_categories_name UNIQUE (name),
  CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE category_moderators (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_category_moderators UNIQUE (category_id, user_id)
);

CREATE TABLE posts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  author_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  summary VARCHAR(500) NOT NULL DEFAULT '',
  content CLOB NOT NULL,
  cover_url VARCHAR(512) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL,
  view_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  dislike_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  published_at TIMESTAMP NULL,
  hidden_reason VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE post_tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  tag VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_post_tags UNIQUE (post_id, tag)
);

CREATE TABLE post_reactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reaction VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_post_reactions UNIQUE (post_id, user_id)
);

CREATE TABLE favorites (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_favorites UNIQUE (post_id, user_id)
);

CREATE TABLE follows (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_follows UNIQUE (follower_id, following_id)
);

CREATE TABLE comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'visible',
  hidden_reason VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE sensitive_words (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  word VARCHAR(100) NOT NULL,
  level VARCHAR(20) NOT NULL,
  hit_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_sensitive_words_word UNIQUE (word)
);

CREATE TABLE sensitive_word_hits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  word_id BIGINT NOT NULL,
  resource_type VARCHAR(40) NOT NULL,
  resource_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  level VARCHAR(20) NOT NULL,
  snippet VARCHAR(300) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE file_objects (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  bucket VARCHAR(100) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  url VARCHAR(1024) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_file_objects_bucket_key UNIQUE (bucket, object_key)
);
