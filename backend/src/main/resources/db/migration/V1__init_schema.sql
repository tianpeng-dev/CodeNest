CREATE TABLE users (
  id BIGSERIAL,
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
  PRIMARY KEY (id),
  CONSTRAINT uk_users_clerk_user_id UNIQUE (clerk_user_id),
  CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE categories (
  id BIGSERIAL,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  cover_url VARCHAR(512) NOT NULL DEFAULT '',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  post_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_categories_name UNIQUE (name),
  CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE category_moderators (
  id BIGSERIAL,
  category_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_category_moderators UNIQUE (category_id, user_id),
  CONSTRAINT fk_category_moderators_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
  CONSTRAINT fk_category_moderators_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_category_moderators_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE posts (
  id BIGSERIAL,
  author_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  summary VARCHAR(500) NOT NULL DEFAULT '',
  content TEXT NOT NULL,
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
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE TABLE post_tags (
  id BIGSERIAL,
  post_id BIGINT NOT NULL,
  tag VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_post_tags UNIQUE (post_id, tag),
  CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE post_reactions (
  id BIGSERIAL,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reaction VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_post_reactions UNIQUE (post_id, user_id),
  CONSTRAINT fk_post_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_post_reactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE favorites (
  id BIGSERIAL,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_favorites UNIQUE (post_id, user_id),
  CONSTRAINT fk_favorites_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE follows (
  id BIGSERIAL,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_follows UNIQUE (follower_id, following_id),
  CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE comments (
  id BIGSERIAL,
  post_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'visible',
  hidden_reason VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE RESTRICT,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE messages (
  id BIGSERIAL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE notifications (
  id BIGSERIAL,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE sensitive_words (
  id BIGSERIAL,
  word VARCHAR(100) NOT NULL,
  level VARCHAR(20) NOT NULL,
  hit_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sensitive_words_word UNIQUE (word),
  CONSTRAINT fk_sensitive_words_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE sensitive_word_hits (
  id BIGSERIAL,
  word_id BIGINT NOT NULL,
  resource_type VARCHAR(40) NOT NULL,
  resource_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  level VARCHAR(20) NOT NULL,
  snippet VARCHAR(300) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_sensitive_word_hits_word FOREIGN KEY (word_id) REFERENCES sensitive_words (id) ON DELETE RESTRICT,
  CONSTRAINT fk_sensitive_word_hits_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE audit_logs (
  id BIGSERIAL,
  operator_id BIGINT NOT NULL,
  action VARCHAR(80) NOT NULL,
  resource_type VARCHAR(40) NOT NULL,
  resource_id BIGINT NOT NULL,
  detail_json TEXT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_logs_operator FOREIGN KEY (operator_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE file_objects (
  id BIGSERIAL,
  owner_id BIGINT NOT NULL,
  bucket VARCHAR(100) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  url VARCHAR(1024) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_file_objects_bucket_key UNIQUE (bucket, object_key),
  CONSTRAINT fk_file_objects_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_categories_status_sort ON categories (status, sort_order);
CREATE INDEX idx_category_moderators_user ON category_moderators (user_id);
CREATE INDEX idx_category_moderators_assigned_by ON category_moderators (assigned_by);
CREATE INDEX idx_posts_status_published ON posts (status, published_at);
CREATE INDEX idx_posts_author_status ON posts (author_id, status);
CREATE INDEX idx_posts_category_status ON posts (category_id, status);
CREATE INDEX ft_posts_title_summary_content ON posts USING GIN (
  to_tsvector('simple', title || ' ' || summary || ' ' || content)
);
CREATE INDEX idx_post_tags_tag ON post_tags (tag);
CREATE INDEX idx_post_reactions_user ON post_reactions (user_id);
CREATE INDEX idx_favorites_user ON favorites (user_id, created_at);
CREATE INDEX idx_follows_following ON follows (following_id);
CREATE INDEX idx_comments_post_status_created ON comments (post_id, status, created_at);
CREATE INDEX idx_comments_author ON comments (author_id, created_at);
CREATE INDEX idx_messages_pair_created ON messages (sender_id, receiver_id, created_at);
CREATE INDEX idx_messages_receiver_read ON messages (receiver_id, read_at);
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at);
CREATE INDEX idx_sensitive_words_level ON sensitive_words (level);
CREATE INDEX idx_sensitive_words_created_by ON sensitive_words (created_by);
CREATE INDEX idx_sensitive_word_hits_resource ON sensitive_word_hits (resource_type, resource_id);
CREATE INDEX idx_sensitive_word_hits_user_created ON sensitive_word_hits (user_id, created_at);
CREATE INDEX idx_sensitive_word_hits_word ON sensitive_word_hits (word_id);
CREATE INDEX idx_audit_logs_operator_created ON audit_logs (operator_id, created_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_file_objects_owner ON file_objects (owner_id, created_at);
