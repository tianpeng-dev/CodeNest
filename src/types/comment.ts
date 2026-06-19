import type { ID } from './common';
import type { User } from './user';

export interface Comment {
  id: ID;
  postId: ID;
  author: User;
  content: string;
  createdAt: string;
}
