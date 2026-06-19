import type { AdminMetric, SensitiveWord } from '../types/admin';
import type { ApiResponse, PageResult } from '../types/common';
import type { Post, PostQuery } from '../types/post';
import type { User } from '../types/user';
import { http, unwrap } from './http';

export async function getAdminMetrics(): Promise<AdminMetric[]> {
  return unwrap(await http.get<ApiResponse<AdminMetric[]>>('/admin/metrics'));
}

export async function getAdminUsers(): Promise<User[]> {
  return unwrap(await http.get<ApiResponse<User[]>>('/admin/users'));
}

export async function getAdminPosts(
  query: PostQuery = {},
): Promise<PageResult<Post>> {
  return unwrap(
    await http.get<ApiResponse<PageResult<Post>>>('/admin/posts', {
      params: query,
    }),
  );
}

export async function getSensitiveWords(): Promise<SensitiveWord[]> {
  return unwrap(
    await http.get<ApiResponse<SensitiveWord[]>>('/admin/sensitive-words'),
  );
}
