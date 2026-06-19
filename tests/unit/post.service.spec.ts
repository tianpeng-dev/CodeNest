import { describe, expect, it, beforeAll, beforeEach } from 'vitest';
import { resetMockApi, setupMockApi } from '@/mocks/mock';
import { getAdminMetrics } from '@/services/admin.service';
import { createDraft, getPosts, getPostById } from '@/services/post.service';

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
});
