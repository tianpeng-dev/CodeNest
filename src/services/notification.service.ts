import type { ApiResponse } from '../types/common';
import type { NotificationItem } from '../types/notification';
import { http, unwrap } from './http';

export async function getNotifications(): Promise<NotificationItem[]> {
  return unwrap(
    await http.get<ApiResponse<NotificationItem[]>>('/notifications'),
  );
}

export async function markNotificationRead(
  id: string,
): Promise<NotificationItem> {
  return unwrap(
    await http.post<ApiResponse<NotificationItem>>(`/notifications/${id}/read`),
  );
}
