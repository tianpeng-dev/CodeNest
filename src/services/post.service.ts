import type { ApiResponse, PageResult } from '../types/common';
import type { Post, PostDraftPayload, PostQuery } from '../types/post';
import { http, unwrap } from './http';

export async function getPosts(
  query: PostQuery = {},
): Promise<PageResult<Post>> {
  return unwrap(
    await http.get<ApiResponse<PageResult<Post>>>('/posts', { params: query }),
  );
}

export async function getPostById(id: string): Promise<Post> {
  return unwrap(await http.get<ApiResponse<Post>>(`/posts/${id}`));
}

export async function createDraft(payload: PostDraftPayload): Promise<Post> {
  return unwrap(await http.post<ApiResponse<Post>>('/posts/drafts', payload));
}

export async function publishPost(id: string): Promise<Post> {
  return unwrap(await http.post<ApiResponse<Post>>(`/posts/${id}/publish`));
}

export async function updatePost(
  id: string,
  payload: PostDraftPayload,
): Promise<Post> {
  return unwrap(await http.put<ApiResponse<Post>>(`/posts/${id}`, payload));
}

export async function deletePost(id: string): Promise<Post> {
  return unwrap(await http.delete<ApiResponse<Post>>(`/posts/${id}`));
}

export async function togglePostAction(
  id: string,
  action: 'like' | 'dislike' | 'favorite',
): Promise<Post> {
  return unwrap(await http.post<ApiResponse<Post>>(`/posts/${id}/${action}`));
}
