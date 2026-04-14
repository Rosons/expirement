import type { BackendHistoryMessage, ChatRole, UiChatMessage } from '../types/chat';

const API_BASE_URL = (import.meta.env.VITE_API_BASE as string | undefined)?.trim() ?? '';

function buildApiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

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

  // JSON能解析但没有命中常见字段时，至少保证前端可见，便于排查流格式
  return data;
}

function mapMessageTypeToRole(messageType?: string): ChatRole {
  if (!messageType) {
    return 'assistant';
  }

  const normalizedType = messageType.toLowerCase();
  if (normalizedType.includes('user')) {
    return 'user';
  }
  if (normalizedType.includes('system')) {
    return 'system';
  }
  return 'assistant';
}

function resolveHistoryText(rawMessage: BackendHistoryMessage): string {
  const candidate =
    rawMessage.text ??
    rawMessage.content ??
    rawMessage.message ??
    rawMessage.metadata?.text ??
    rawMessage.metadata?.content;

  return typeof candidate === 'string' ? candidate : '';
}

export async function fetchConversations(): Promise<string[]> {
  const response = await fetch(buildApiUrl('/ai/chat/conversations'), {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`加载会话列表失败：${response.status}`);
  }

  const data = (await response.json()) as unknown;
  return Array.isArray(data) ? (data as string[]) : [];
}

export async function fetchHistory(chatId: string): Promise<UiChatMessage[]> {
  const query = new URLSearchParams({ chatId });
  const response = await fetch(buildApiUrl(`/ai/chat/history?${query.toString()}`), {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`加载历史消息失败：${response.status}`);
  }

  const data = (await response.json()) as unknown;
  if (!Array.isArray(data)) {
    return [];
  }

  return (data as BackendHistoryMessage[]).map((item, index) => ({
    id: `${chatId}-history-${index}`,
    role: mapMessageTypeToRole(item.messageType),
    content: resolveHistoryText(item),
    createdAt: Date.now() + index,
  }));
}

export async function streamChatResponse(
  chatId: string,
  message: string,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const query = new URLSearchParams({ chatId, message });
  const response = await fetch(buildApiUrl(`/ai/chat?${query.toString()}`), {
    method: 'GET',
    headers: {
      Accept: 'text/plain, text/html, text/event-stream',
    },
    cache: 'no-store',
    signal,
  });

  if (!response.ok) {
    const responseText = await response.text().catch(() => '');
    throw new Error(
      `发送消息失败：${response.status}${responseText ? `，响应：${responseText.slice(0, 120)}` : ''}`,
    );
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
        // SSE 规范允许 data: 后有一个可选空格，仅移除这一个，保留其余缩进
        if (payload.startsWith(' ')) {
          payload = payload.slice(1);
        }
        sseDataLines.push(payload);
        continue;
      }

      if (line.startsWith(':')) {
        continue;
      }

      // 兜底：非标准SSE也尽量显示文本
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
