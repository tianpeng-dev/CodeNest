import type { ID } from './common';
import type { UserStatus } from './user';

export interface AdminMetric {
  label: string;
  value: number;
  trend: number;
}

export interface SensitiveWord {
  id: ID;
  word: string;
  level: 'low' | 'medium' | 'high';
  createdAt: string;
  hitCount: number;
}

export interface AdminModerator {
  id: ID;
  username: string;
  displayName: string;
  status: UserStatus;
  avatarUrl: string;
}

export interface AdminModeratorSection {
  id: ID;
  sectionName: string;
  description: string;
  moderatorCount: number;
  moderators: AdminModerator[];
  updatedAt: string;
}
