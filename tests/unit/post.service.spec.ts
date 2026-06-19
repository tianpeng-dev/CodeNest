import { describe, expect, it, beforeAll, beforeEach } from 'vitest';
import { resetMockApi, setupMockApi } from '@/mocks/mock';
import { getAdminMetrics } from '@/services/admin.service';
import { getNotifications, markNotificationRead } from '@/services/notification.service';
import { getThreads, getThreadMessages, mockSendMessage } from '@/services/message.service';
import {
  createDraft,
  deletePost,
  getPosts,
  getPostById,
  publishPost,
  togglePostAction,
  updatePost,
} from '@/services/post.service';

beforeAll(() => {
  setupMockApi();
});

beforeEach(() => {
  resetMockApi();
  window.localStorage.clear();
});

describe('post service', () => {
  it('returns paginated posts', async () => {
    const result = await getPosts({ page: 1, pageSize: 3 });
    expect(result.items).toHaveLength(3);
    expect(result.total).toBeGreaterThanOrEqual(6);
  });

  it('returns one post by id', async () => {
    const list = await getPosts({ page: 1, pageSize: 1 });
    const post = await getPostById(list.items[0].id);
    expect(post.id).toBe(list.items[0].id);
  });

  it('filters posts by category slug and author id', async () => {
    const byCategory = await getPosts({
      categorySlug: 'frontend',
      status: 'published',
      page: 1,
      pageSize: 10,
    });
    expect(byCategory.items.length).toBeGreaterThan(0);
    expect(byCategory.items.every((post) => post.category.slug === 'frontend')).toBe(true);

    const byAuthor = await getPosts({
      authorId: 'user-001',
      status: 'published',
      page: 1,
      pageSize: 10,
    });
    expect(byAuthor.items.length).toBeGreaterThan(0);
    expect(byAuthor.items.every((post) => post.author.id === 'user-001')).toBe(true);
  });

  it('updates post interaction counts through the action service', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-admin');
    const post = await getPostById('post-001');

    const liked = await togglePostAction(post.id, 'like');
    expect(liked.likeCount).toBe(post.likeCount + 1);

    const favorited = await togglePostAction(post.id, 'favorite');
    expect(favorited.favoriteCount).toBe(post.favoriteCount + 1);
  });

  it('marks notifications read and appends mock messages', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-admin');

    const notifications = await getNotifications();
    const unread = notifications.find((notification) => !notification.readAt);
    expect(unread).toBeTruthy();

    const readNotification = await markNotificationRead(unread!.id);
    expect(readNotification.readAt).toBeTruthy();

    const threads = await getThreads();
    const thread = threads[0];
    const before = await getThreadMessages(thread.id);
    const sent = await mockSendMessage(thread.id, '收到，我稍后整理。');
    const after = await getThreadMessages(thread.id);

    expect(sent.content).toBe('收到，我稍后整理。');
    expect(after).toHaveLength(before.length + 1);
    expect(after.at(-1)?.id).toBe(sent.id);
  });

  it('rejects protected calls with invalid tokens', async () => {
    window.localStorage.setItem('codenest_token', 'bad-token');

    await expect(
      createDraft({
        title: 'Invalid token draft',
        summary: 'Should not be created',
        content: 'Protected endpoints reject stale tokens.',
        coverUrl: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085',
        categoryId: 'cat-frontend',
        tags: ['Auth'],
        status: 'draft',
      }),
    ).rejects.toThrow('请先登录');
  });

  it('rejects admin calls for normal users', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-writer');

    await expect(getAdminMetrics()).rejects.toThrow('无权访问');
  });

  it('rejects post mutations from non-owners', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-writer');
    const adminPost = await getPostById('post-001');
    const payload = {
      title: 'Unauthorized update',
      summary: 'Writer should not update admin posts',
      content: adminPost.content,
      coverUrl: adminPost.coverUrl,
      categoryId: adminPost.category.id,
      tags: adminPost.tags,
      status: 'draft' as const,
    };

    await expect(updatePost(adminPost.id, payload)).rejects.toThrow('无权访问');
    await expect(publishPost(adminPost.id)).rejects.toThrow('无权访问');
    await expect(deletePost(adminPost.id)).rejects.toThrow('无权访问');
  });

  it('allows admins to mutate any post', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-admin');
    const writerPost = await getPostById('post-002');
    const updated = await updatePost(writerPost.id, {
      title: 'Admin adjusted title',
      summary: writerPost.summary,
      content: writerPost.content,
      coverUrl: writerPost.coverUrl,
      categoryId: writerPost.category.id,
      tags: writerPost.tags,
      status: 'draft',
    });

    expect(updated.title).toBe('Admin adjusted title');
  });
});
