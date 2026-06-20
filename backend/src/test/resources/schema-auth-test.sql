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
