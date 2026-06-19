import type { ApiResponse } from '../types/common';
import type { User } from '../types/user';
import { http, unwrap } from './http';

export async function getUserProfile(id: string): Promise<User> {
  return unwrap(await http.get<ApiResponse<User>>(`/users/${id}`));
}

export async function toggleFollow(id: string): Promise<User> {
  return unwrap(await http.post<ApiResponse<User>>(`/users/${id}/follow`));
}
