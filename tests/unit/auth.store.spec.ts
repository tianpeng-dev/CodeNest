import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { canAccessRoute } from '@/app/permissions';
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

  it('rejects invalid credentials with the service message', async () => {
    const authStore = useAuthStore();

    await expect(
      authStore.login({ username: 'writer', password: 'wrong-password' }),
    ).rejects.toThrow('用户名或密码错误');

    expect(authStore.token).toBeNull();
    expect(authStore.currentUser).toBeNull();
  });

  it('registers a new user and persists the session token', async () => {
    const authStore = useAuthStore();

    await authStore.register({
      displayName: '新作者',
      username: 'new-writer',
      password: 'password123',
    });

    expect(authStore.token).toBe('mock-token-new-writer');
    expect(authStore.currentUser?.username).toBe('new-writer');
    expect(window.localStorage.getItem('codenest_token')).toBe('mock-token-new-writer');
  });

  it('rejects duplicate usernames with the service message', async () => {
    const authStore = useAuthStore();

    await expect(
      authStore.register({
        displayName: '重复用户',
        username: 'writer',
        password: 'password123',
      }),
    ).rejects.toThrow('用户名已存在');

    expect(authStore.token).toBeNull();
    expect(authStore.currentUser).toBeNull();
  });

  it('loads the quick-fill writer as an active normal user', async () => {
    window.localStorage.setItem('codenest_token', 'mock-token-writer');
    const authStore = useAuthStore();

    await authStore.loadCurrentUser();

    expect(authStore.currentUser?.username).toBe('chen-dev');
    expect(authStore.currentUser?.role).toBe('user');
    expect(authStore.currentUser?.status).toBe('active');
    expect(authStore.isLoggedIn).toBe(true);
    expect(authStore.isAdmin).toBe(false);
    expect(canAccessRoute('user', authStore.currentUser)).toBe(true);
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
