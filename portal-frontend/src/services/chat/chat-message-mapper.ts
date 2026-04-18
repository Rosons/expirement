import type { ChatMessageHistoryVo, ChatRole, UiChatMessage } from '../../types/chat';

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

function parseBackendDateToMillis(iso?: string | null): number {
  if (!iso) {
    return Date.now();
  }
  const t = Date.parse(iso);
  return Number.isNaN(t) ? Date.now() : t;
}

function resolveMessageBody(row: ChatMessageHistoryVo): string {
  const main = typeof row.content === 'string' ? row.content.trim() : '';
  if (main) {
    return row.content ?? '';
  }
  const parts = row.parts ?? [];
  const textPart = parts.find((p) => p.partType === 'TEXT' && p.contentText?.trim());
  if (textPart?.contentText) {
    return textPart.contentText;
  }
  const anyText = parts.find((p) => p.contentText?.trim());
  return anyText?.contentText ?? '';
}

export function mapHistoryRecordsToUi(records: ChatMessageHistoryVo[], chatId: string): UiChatMessage[] {
  return records.map((item, index) => ({
    id: item.id != null ? `hist-${item.id}` : `${chatId}-history-${index}`,
    role: mapMessageTypeToRole(item.role),
    content: resolveMessageBody(item),
    createdAt: parseBackendDateToMillis(item.createdAt),
    parts: item.parts && item.parts.length > 0 ? item.parts : undefined,
  }));
}
