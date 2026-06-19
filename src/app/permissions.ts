import type { User } from '@/types/user';

export type RouteAccess = 'public' | 'user' | 'admin' | undefined;

export function canAccessRoute(access: RouteAccess, user: User | null | undefined) {
  if (!access || access === 'public') return true;
  if (!user) return false;
  if (access === 'user') return user.status === 'active';
  return user.role === 'admin' && user.status === 'active';
}
