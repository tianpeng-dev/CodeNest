import type { ApiResponse } from '../types/common';
import type { MessageItem, MessageThread } from '../types/message';
import { http, unwrap } from './http';

export async function getThreads(): Promise<MessageThread[]> {
  return unwrap(
    await http.get<ApiResponse<MessageThread[]>>('/messages/threads'),
  );
}

export async function getThreadMessages(
  threadId: string,
): Promise<MessageItem[]> {
  return unwrap(
    await http.get<ApiResponse<MessageItem[]>>(
      `/messages/threads/${threadId}`,
    ),
  );
}

export async function mockSendMessage(
  threadId: string,
  content: string,
): Promise<MessageItem> {
  return unwrap(
    await http.post<ApiResponse<MessageItem>>(`/messages/threads/${threadId}`, {
      content,
    }),
  );
}
