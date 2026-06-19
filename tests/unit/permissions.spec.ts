import { describe, expect, it } from 'vitest';
import { canAccessRoute } from '@/app/permissions';
import type { User } from '@/types/user';

const activeUser: User = {
  id: 'user-101',
  username: 'writer',
  displayName: 'Writer',
  avatarUrl: '',
  bio: '',
  role: 'user',
  status: 'active',
  muteUntil: null,
  postCount: 0,
  likeCount: 0,
  favoriteCount: 0,
  followerCount: 0,
};

const activeAdmin: User = {
  ...activeUser,
  id: 'user-102',
  username: 'admin',
  role: 'admin',
};

const bannedUser: User = {
  ...activeUser,
  id: 'user-103',
  username: 'muted-writer',
  status: 'banned',
  muteUntil: '2026-07-01T09:00:00.000Z',
};

const bannedAdmin: User = {
  ...activeAdmin,
  id: 'user-104',
  username: 'muted-admin',
  status: 'banned',
  muteUntil: '2026-07-01T09:00:00.000Z',
};

describe('canAccessRoute', () => {
  it('allows a public route for a guest', () => {
    expect(canAccessRoute('public', null)).toBe(true);
  });

  it('blocks a user route for a guest', () => {
    expect(canAccessRoute('user', null)).toBe(false);
  });

  it('blocks an admin route for a normal user', () => {
    expect(canAccessRoute('admin', activeUser)).toBe(false);
  });

  it('allows an admin route for an active admin', () => {
    expect(canAccessRoute('admin', activeAdmin)).toBe(true);
  });

  it('blocks a user route for a banned normal user', () => {
    expect(canAccessRoute('user', bannedUser)).toBe(false);
  });

  it('blocks an admin route for a banned admin', () => {
    expect(canAccessRoute('admin', bannedAdmin)).toBe(false);
  });
});
