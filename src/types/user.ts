import type { ID } from './common';

export type UserRole = 'user' | 'admin';

export type UserStatus = 'active' | 'banned';

export interface User {
  id: ID;
  username: string;
  displayName: string;
  avatarUrl: string;
  bio: string;
  role: UserRole;
  status: UserStatus;
  muteUntil: string | null;
  postCount: number;
  likeCount: number;
  favoriteCount: number;
  followerCount: number;
}
