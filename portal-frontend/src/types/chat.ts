export type ChatRole = 'user' | 'assistant' | 'system';

export interface UiChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  createdAt: number;
  streaming?: boolean;
}

export interface BackendHistoryMessage {
  messageType?: string;
  text?: string;
  content?: string;
  message?: string;
  metadata?: Record<string, unknown>;
}
