import { getChatStreamUrl, getKnowledgeChatStreamUrl } from '../../api/chat-endpoints';
import { notifyRequestFailure } from '../../feedback';
import type { ChatStreamQueryRequest } from '../../types/chat';

function findFirstNonEmptyText(value: unknown, depth = 0): string {
  if (depth > 6 || value == null) {
    return '';
  }

  if (typeof value === 'string') {
    return value.length > 0 ? value : '';
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findFirstNonEmptyText(item, depth + 1);
      if (found) {
        return found;
      }
    }
    return '';
  }

  if (typeof value === 'object') {
    const objectValue = value as Record<string, unknown>;
    const priorityKeys = ['content', 'text', 'message', 'answer', 'output', 'delta', 'result', 'data'];

    for (const key of priorityKeys) {
      if (key in objectValue) {
        const found = findFirstNonEmptyText(objectValue[key], depth + 1);
        if (found) {
          return found;
        }
      }
    }
  }

  return '';
}

function extractTextFromSseData(data: string): string {
  const trimmedForControl = data.trim();
  if (!trimmedForControl || trimmedForControl === '[DONE]') {
    return '';
  }

  try {
    const parsed = JSON.parse(data) as unknown;
    const extracted = findFirstNonEmptyText(parsed);
    if (extracted) {
      return extracted;
    }
  } catch {
    return data;
  }

  return data;
}

export async function streamChatResponse(
  query: ChatStreamQueryRequest,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  try {
    await runStreamChatResponse(query, onChunk, getChatStreamUrl(), signal);
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    notifyRequestFailure(error);
    throw error;
  }
}

export async function streamChatResponseByUrl(
  query: ChatStreamQueryRequest,
  streamUrl: string,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  try {
    await runStreamChatResponse(query, onChunk, streamUrl, signal);
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    notifyRequestFailure(error);
    throw error;
  }
}

export async function streamKnowledgeChatResponse(
  query: ChatStreamQueryRequest,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  try {
    await runStreamChatResponse(query, onChunk, getKnowledgeChatStreamUrl(), signal);
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    notifyRequestFailure(error);
    throw error;
  }
}

async function runStreamChatResponse(
  query: ChatStreamQueryRequest,
  onChunk: (chunk: string) => void,
  streamUrl: string,
  signal?: AbortSignal,
): Promise<void> {
  const params = new URLSearchParams({ chatId: query.chatId, message: query.message });
  if (query.type?.trim()) {
    params.set('type', query.type.trim());
  }
  const response = await fetch(`${streamUrl}?${params.toString()}`, {
    method: 'GET',
    headers: {
      Accept: 'text/plain, text/html, text/event-stream',
    },
    cache: 'no-store',
    signal,
  });

  if (!response.ok) {
    const responseText = await response.text().catch(() => '');
    throw new Error(`发送消息失败：${response.status}${responseText ? `，响应：${responseText.slice(0, 120)}` : ''}`);
  }

  if (!response.body) {
    const text = await response.text();
    if (text) {
      onChunk(text);
      return;
    }
    throw new Error('服务端未返回可读取内容');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  const contentType = response.headers.get('content-type')?.toLowerCase() ?? '';
  let shouldParseAsSse = contentType.includes('text/event-stream');
  let lineBuffer = '';
  let sseDataLines: string[] = [];

  const flushSseEvent = (): void => {
    if (!sseDataLines.length) {
      return;
    }
    const eventPayload = sseDataLines.join('\n');
    sseDataLines = [];
    const parsed = extractTextFromSseData(eventPayload);
    if (parsed) {
      onChunk(parsed);
    }
  };

  const processAsSse = (text: string): void => {
    lineBuffer += text;
    const lines = lineBuffer.split(/\r?\n/);
    lineBuffer = lines.pop() ?? '';

    for (const line of lines) {
      if (!line) {
        flushSseEvent();
        continue;
      }

      if (line.startsWith('data:')) {
        let payload = line.slice(5);
        if (payload.startsWith(' ')) {
          payload = payload.slice(1);
        }
        sseDataLines.push(payload);
        continue;
      }

      if (line.startsWith(':')) {
        continue;
      }

      if (!sseDataLines.length && line.trim()) {
        onChunk(line);
      }
    }
  };

  while (true) {
    if (signal?.aborted) {
      await reader.cancel().catch(() => undefined);
      throw new DOMException('The operation was aborted.', 'AbortError');
    }

    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    if (value) {
      const decodedText = decoder.decode(value, { stream: true });
      if (!decodedText) {
        continue;
      }

      if (!shouldParseAsSse && /(^|[\r\n])data:/.test(decodedText)) {
        shouldParseAsSse = true;
      }

      if (shouldParseAsSse) {
        processAsSse(decodedText);
      } else {
        onChunk(decodedText);
      }
    }
  }

  const tailText = decoder.decode();
  if (shouldParseAsSse) {
    if (tailText) {
      processAsSse(tailText);
    }
    if (lineBuffer.startsWith('data:')) {
      let payload = lineBuffer.slice(5);
      if (payload.startsWith(' ')) {
        payload = payload.slice(1);
      }
      sseDataLines.push(payload);
      lineBuffer = '';
    } else if (lineBuffer.trim()) {
      onChunk(lineBuffer);
      lineBuffer = '';
    }
    flushSseEvent();
  } else if (tailText) {
    onChunk(tailText);
  }
}
