import type { Comment } from '../types/comment';
import type { ApiResponse } from '../types/common';
import { http, unwrap } from './http';

export async function getComments(postId: string): Promise<Comment[]> {
  return unwrap(
    await http.get<ApiResponse<Comment[]>>(`/posts/${postId}/comments`),
  );
}

export async function createComment(
  postId: string,
  content: string,
): Promise<Comment> {
  return unwrap(
    await http.post<ApiResponse<Comment>>(`/posts/${postId}/comments`, {
      content,
    }),
  );
}

export async function deleteComment(id: string): Promise<Comment> {
  return unwrap(await http.delete<ApiResponse<Comment>>(`/comments/${id}`));
}
