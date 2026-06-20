import axios from 'axios';

export type AuthTokenProvider = () => Promise<string | null> | string | null;

declare global {
  interface Window {
    Clerk?: {
      session?: {
        getToken?: AuthTokenProvider;
      };
    };
    __codenestAuthTokenProvider?: AuthTokenProvider;
  }
}

let authTokenProvider: AuthTokenProvider | null = null;

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 8000,
});

export function setAuthTokenProvider(provider: AuthTokenProvider | null) {
  authTokenProvider = provider;
}

async function getProviderToken(provider: AuthTokenProvider | undefined | null): Promise<string | null> {
  if (!provider) {
    return null;
  }

  try {
    return await provider();
  } catch {
    return null;
  }
}

async function getAuthToken(): Promise<string | null> {
  if (typeof window === 'undefined') {
    return null;
  }

  const providerToken =
    (await getProviderToken(authTokenProvider))
    ?? (await getProviderToken(window.__codenestAuthTokenProvider))
    ?? (await getProviderToken(window.Clerk?.session?.getToken));

  return providerToken ?? window.localStorage.getItem('codenest_token');
}

http.interceptors.request.use(async (config) => {
  const token = await getAuthToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export function unwrap<T>(response: {
  data: { code: number; message: string; data: T };
}): T {
  if (response.data.code !== 0) {
    throw new Error(response.data.message);
  }

  return response.data.data;
}
