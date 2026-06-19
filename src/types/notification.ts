import type { ID } from './common';

export interface NotificationItem {
  id: ID;
  title: string;
  content: string;
  readAt: string | null;
  createdAt: string;
}
