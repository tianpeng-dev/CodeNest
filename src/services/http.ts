import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.request.use((config) => {
  const token =
    typeof window === 'undefined'
      ? null
      : window.localStorage.getItem('codenest_token');

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
