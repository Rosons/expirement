import type { ConversationListItem, UiChatMessage } from '../../../../types/chat';

function createMessageId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `msg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export function createChatId(): string {
  return `chat-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function buildUiMessage(role: UiChatMessage['role'], content = '', streaming = false): UiChatMessage {
  return {
    id: createMessageId(),
    role,
    content,
    createdAt: Date.now(),
    streaming,
  };
}

export function shortenChatId(chatId: string): string {
  if (chatId.length <= 30) {
    return chatId;
  }
  return `${chatId.slice(0, 12)}...${chatId.slice(-8)}`;
}

export function getConversationTitle(item: ConversationListItem, index: number, currentChatId: string): string {
  if (item.id === currentChatId) {
    return '当前会话';
  }
  const title = item.title?.trim();
  if (title) {
    return title;
  }
  return `会话 ${String(index + 1).padStart(2, '0')}`;
}

export function getConversationSubtitle(item: ConversationListItem): string {
  return shortenChatId(item.id);
}

export function appendAssistantChunk(messages: UiChatMessage[], messageId: string, chunk: string): void {
  const targetIndex = messages.findIndex((item) => item.id === messageId);
  if (targetIndex < 0) {
    return;
  }
  const targetMessage = messages[targetIndex];
  messages[targetIndex] = {
    ...targetMessage,
    content: `${targetMessage.content}${chunk}`,
  };
}

export function finalizeAssistantMessage(
  messages: UiChatMessage[],
  messageId: string,
  fallbackContent?: string,
): boolean {
  const targetIndex = messages.findIndex((item) => item.id === messageId);
  if (targetIndex < 0) {
    return false;
  }

  const targetMessage = messages[targetIndex];
  const finalContent = targetMessage.content || fallbackContent || '';
  messages[targetIndex] = {
    ...targetMessage,
    streaming: false,
    content: finalContent,
  };

  return finalContent.trim().length > 0;
}
