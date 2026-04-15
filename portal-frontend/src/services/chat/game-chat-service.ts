import { getGameChatStreamUrl } from '../../api';
import type { ChatStreamQueryRequest } from '../../types/chat';
import { streamChatResponseByUrl } from './chat-service';

export async function streamGameChatResponse(
  query: ChatStreamQueryRequest,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  await streamChatResponseByUrl(query, getGameChatStreamUrl(), onChunk, signal);
}
