import { defineStore } from 'pinia';
import * as authService from '@/services/auth.service';
import type { LoginPayload, RegisterPayload } from '@/types/auth';
import type { User } from '@/types/user';

const tokenKey = 'codenest_token';

function readStoredToken() {
  return typeof window === 'undefined' ? null : window.localStorage.getItem(tokenKey);
}

function persistToken(token: string | null) {
  if (typeof window === 'undefined') return;

  if (token) {
    window.localStorage.setItem(tokenKey, token);
  } else {
    window.localStorage.removeItem(tokenKey);
  }
}

interface AuthState {
  token: string | null;
  currentUser: User | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: readStoredToken(),
    currentUser: null,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => {
      return state.currentUser?.role === 'admin' && state.currentUser.status === 'active';
    },
  },
  actions: {
    setAuth(token: string, user: User) {
      this.token = token;
      this.currentUser = user;
      persistToken(token);
    },
    clearAuth() {
      this.token = null;
      this.currentUser = null;
      persistToken(null);
    },
    async login(payload: LoginPayload) {
      try {
        const session = await authService.login(payload);
        this.setAuth(session.token, session.user);
        return session;
      } catch (error) {
        this.clearAuth();
        throw error;
      }
    },
    async register(payload: RegisterPayload) {
      try {
        const session = await authService.register(payload);
        this.setAuth(session.token, session.user);
        return session;
      } catch (error) {
        this.clearAuth();
        throw error;
      }
    },
    async logout() {
      try {
        if (this.token) {
          await authService.logout();
        }
      } finally {
        this.clearAuth();
      }
    },
    async loadCurrentUser() {
      if (!this.token) {
        this.clearAuth();
        return null;
      }

      try {
        const user = await authService.getCurrentUser();
        this.currentUser = user;
        return user;
      } catch (error) {
        this.clearAuth();
        throw error;
      }
    },
  },
});
