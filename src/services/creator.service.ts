import type { PiePoint, TrendPoint } from '../types/analytics';
import type { Comment } from '../types/comment';
import type { ApiResponse, PageResult } from '../types/common';
import type { Post, PostQuery } from '../types/post';
import { http, unwrap } from './http';

export interface CreatorComment extends Comment {
  post: Pick<Post, 'id' | 'title'>;
}

export interface CreatorColumn {
  id: string;
  title: string;
  description: string;
  postCount: number;
  coverUrl: string;
  updatedAt: string;
}

export interface CreatorAnalytics {
  postCount: number;
  publishedCount: number;
  draftCount: number;
  totalViews: number;
  totalLikes: number;
  totalFavorites: number;
  trend: TrendPoint[];
  pie: PiePoint[];
}

export async function getCreatorPosts(
  query: PostQuery = {},
): Promise<PageResult<Post>> {
  return unwrap(
    await http.get<ApiResponse<PageResult<Post>>>('/creator/posts', {
      params: query,
    }),
  );
}

export async function getCreatorAnalytics(): Promise<CreatorAnalytics> {
  return unwrap(
    await http.get<ApiResponse<CreatorAnalytics>>('/creator/analytics'),
  );
}

export async function getCreatorComments(): Promise<CreatorComment[]> {
  return unwrap(
    await http.get<ApiResponse<CreatorComment[]>>('/creator/comments'),
  );
}

export async function getCreatorColumns(): Promise<CreatorColumn[]> {
  return unwrap(
    await http.get<ApiResponse<CreatorColumn[]>>('/creator/columns'),
  );
}
