INSERT INTO users (
  id,
  clerk_user_id,
  username,
  display_name,
  avatar_url,
  bio,
  role,
  status,
  created_at,
  updated_at
) VALUES
  (1, 'user_dev_admin_0001', 'admin', 'CodeNest Admin', '', 'Development administrator account.', 'admin', 'active', '2026-06-20 00:00:00', '2026-06-20 00:00:00'),
  (2, 'user_dev_writer_0001', 'writer', 'CodeNest Writer', '', 'Development writer account.', 'user', 'active', '2026-06-20 00:00:00', '2026-06-20 00:00:00');

INSERT INTO categories (
  id,
  name,
  slug,
  description,
  cover_url,
  sort_order,
  status,
  created_at,
  updated_at
) VALUES
  (1, 'Engineering', 'engineering', 'Backend, frontend, DevOps, and systems notes.', '', 10, 'active', '2026-06-20 00:00:00', '2026-06-20 00:00:00'),
  (2, 'Product', 'product', 'Product thinking, planning, and launch lessons.', '', 20, 'active', '2026-06-20 00:00:00', '2026-06-20 00:00:00'),
  (3, 'Community', 'community', 'Community updates, discussion, and member stories.', '', 30, 'active', '2026-06-20 00:00:00', '2026-06-20 00:00:00');

INSERT INTO sensitive_words (
  id,
  word,
  level,
  hit_count,
  created_by,
  created_at,
  updated_at
) VALUES
  (1, 'spoiler-test-low', 'low', 0, 1, '2026-06-20 00:00:00', '2026-06-20 00:00:00'),
  (2, 'review-test-medium', 'medium', 0, 1, '2026-06-20 00:00:00', '2026-06-20 00:00:00'),
  (3, 'block-test-high', 'high', 0, 1, '2026-06-20 00:00:00', '2026-06-20 00:00:00');
