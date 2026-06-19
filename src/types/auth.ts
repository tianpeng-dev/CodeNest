import type { User } from './user';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  displayName: string;
}

export interface AuthSession {
  token: string;
  user: User;
  expiresAt: string;
}
