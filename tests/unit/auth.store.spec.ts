import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { resetMockApi, setupMockApi } from '@/mocks/mock';
import { useAuthStore } from '@/stores/auth.store';

beforeAll(() => {
  setupMockApi();
});

beforeEach(() => {
  resetMockApi();
  window.localStorage.clear();
  setActivePinia(createPinia());
});

describe('auth store', () => {
  it('logs in and persists the session token', async () => {
    const authStore = useAuthStore();

    await authStore.login({ username: 'admin', password: 'admin123' });

    expect(authStore.token).toBe('mock-token-admin');
    expect(authStore.currentUser?.username).toBe('lin-admin');
    expect(authStore.isLoggedIn).toBe(true);
    expect(authStore.isAdmin).toBe(true);
    expect(window.localStorage.getItem('codenest_token')).toBe('mock-token-admin');
  });

  it('logs out and clears auth state', async () => {
    const authStore = useAuthStore();
    await authStore.login({ username: 'admin', password: 'admin123' });

    await authStore.logout();

    expect(authStore.token).toBeNull();
    expect(authStore.currentUser).toBeNull();
    expect(authStore.isLoggedIn).toBe(false);
    expect(window.localStorage.getItem('codenest_token')).toBeNull();
  });

  it('clears stale auth when loading current user fails', async () => {
    window.localStorage.setItem('codenest_token', 'bad-token');
    const authStore = useAuthStore();

    await expect(authStore.loadCurrentUser()).rejects.toThrow('请先登录');

    expect(authStore.token).toBeNull();
    expect(authStore.currentUser).toBeNull();
    expect(window.localStorage.getItem('codenest_token')).toBeNull();
  });
});
