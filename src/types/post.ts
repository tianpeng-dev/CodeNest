import type { ID } from './common';
import type { User } from './user';

export type PostStatus = 'draft' | 'published' | 'hidden' | 'deleted';

export interface Category {
  id: ID;
  name: string;
  slug: string;
  description: string;
  postCount: number;
}

export interface Post {
  id: ID;
  title: string;
  summary: string;
  content: string;
  coverUrl: string;
  author: User;
  category: Category;
  tags: string[];
  status: PostStatus;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface PostQuery {
  keyword?: string;
  categoryId?: ID;
  categorySlug?: string;
  authorId?: ID;
  tags?: string[];
  status?: PostStatus;
  page?: number;
  pageSize?: number;
  sortBy?: 'latest' | 'popular' | 'commented';
}

export interface PostDraftPayload {
  title: string;
  summary: string;
  content: string;
  coverUrl: string;
  categoryId: ID;
  tags: string[];
  status: Extract<PostStatus, 'draft' | 'published'>;
}
