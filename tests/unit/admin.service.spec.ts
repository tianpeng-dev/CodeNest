import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { resetMockApi, setupMockApi } from '@/mocks/mock';
import {
  getAdminCategories,
  getAdminModerators,
} from '@/services/admin.service';

beforeAll(() => {
  setupMockApi();
});

beforeEach(() => {
  resetMockApi();
  window.localStorage.clear();
});

describe('admin service', () => {
  it('returns category and moderator data for admins', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-admin');

    const categories = await getAdminCategories();
    const moderators = await getAdminModerators();

    expect(categories.length).toBeGreaterThan(0);
    expect(categories[0]).toEqual(
      expect.objectContaining({
        id: expect.any(String),
        name: expect.any(String),
        postCount: expect.any(Number),
      }),
    );
    expect(moderators.length).toBeGreaterThan(0);
    expect(moderators[0]).toEqual(
      expect.objectContaining({
        id: expect.any(String),
        sectionName: expect.any(String),
        moderators: expect.any(Array),
      }),
    );
  });

  it('rejects category and moderator calls for normal users', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-writer');

    await expect(getAdminCategories()).rejects.toThrow('无权访问');
    await expect(getAdminModerators()).rejects.toThrow('无权访问');
  });
});
