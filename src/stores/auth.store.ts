import { defineStore } from 'pinia';
import * as authService from '@/services/auth.service';
import type { LoginPayload, RegisterPayload } from '@/types/auth';
import type { User } from '@/types/user';

const tokenKey = 'codenest_token';

function readStoredToken() {
  return typeof window === 'undefined' ? null : window.localStorage.getItem(tokenKey);
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

async function readClerkSessionToken(timeoutMs = 8000) {
  if (typeof window === 'undefined') {
    return null;
  }

  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const clerk = window.Clerk;

    if (!clerk) {
      await wait(25);
      continue;
    }

    if (clerk.loaded && clerk.session === null) {
      return null;
    }

    if (clerk.loaded && clerk.session?.getToken) {
      return clerk.session.getToken();
    }

    const listenerToken = await new Promise<string | null | undefined>((resolve) => {
      let unsubscribe: (() => void) | undefined;
      const timer = window.setTimeout(() => {
        unsubscribe?.();
        resolve(undefined);
      }, Math.max(0, deadline - Date.now()));

      unsubscribe = clerk.addListener(async ({ session }) => {
        if (session === undefined) {
          return;
        }

        window.clearTimeout(timer);
        unsubscribe?.();
        resolve(session?.getToken ? await session.getToken() : null);
      });
    });

    if (listenerToken !== undefined) {
      return listenerToken;
    }
  }

  return null;
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
      const storedToken = readStoredToken();
      const clerkToken = storedToken ? null : await readClerkSessionToken();

      if (!storedToken && !clerkToken) {
        this.clearAuth();
        return null;
      }

      try {
        this.token = storedToken ?? clerkToken;
        const user = storedToken
          ? await authService.getCurrentUser()
          : await authService.syncCurrentUser();
        this.currentUser = user;
        return user;
      } catch (error) {
        this.clearAuth();
        throw error;
      }
    },
  },
});
