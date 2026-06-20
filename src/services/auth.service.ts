import type { AuthSession, LoginPayload, RegisterPayload } from '../types/auth';
import type { ApiResponse } from '../types/common';
import type { User } from '../types/user';
import { http, unwrap } from './http';

function persistSession(session: AuthSession): AuthSession {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem('codenest_token', session.token);
  }

  return session;
}

export async function login(payload: LoginPayload): Promise<AuthSession> {
  return persistSession(
    unwrap(await http.post<ApiResponse<AuthSession>>('/auth/login', payload)),
  );
}

export async function register(payload: RegisterPayload): Promise<AuthSession> {
  return persistSession(
    unwrap(await http.post<ApiResponse<AuthSession>>('/auth/register', payload)),
  );
}

export async function logout(): Promise<null> {
  const result = unwrap(await http.post<ApiResponse<null>>('/auth/logout'));

  if (typeof window !== 'undefined') {
    window.localStorage.removeItem('codenest_token');
  }

  return result;
}

export async function getCurrentUser(): Promise<User> {
  return unwrap(await http.get<ApiResponse<User>>('/auth/me'));
}

export async function syncCurrentUser(): Promise<User> {
  return unwrap(await http.post<ApiResponse<User>>('/auth/sync'));
}
