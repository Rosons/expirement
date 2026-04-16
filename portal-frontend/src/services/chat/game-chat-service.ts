import { getGameChatStreamUrl } from '../../api/chat-endpoints';
import type { ChatStreamQueryRequest } from '../../types/chat';
import { streamChatResponseByUrl } from './chat-stream-service';

export async function streamGameChatResponse(
  query: ChatStreamQueryRequest,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  await streamChatResponseByUrl(query, getGameChatStreamUrl(), onChunk, signal);
}
