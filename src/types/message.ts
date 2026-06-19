import type { ID } from './common';
import type { User } from './user';

export interface MessageThread {
  id: ID;
  participant: User;
  lastMessage: string;
  unreadCount: number;
  updatedAt: string;
}

export interface MessageItem {
  id: ID;
  threadId: ID;
  sender: User;
  content: string;
  readAt: string | null;
  createdAt: string;
}
