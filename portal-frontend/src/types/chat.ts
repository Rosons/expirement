export type ChatRole = 'user' | 'assistant' | 'system';

export interface UiChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  createdAt: number;
  streaming?: boolean;
  /** 消息附件，与后端 ChatMessagePartVo 对齐 */
  parts?: ChatMessagePartVo[];
}

/** 与后端 ChatHistoryQueryRequest 对齐（v1 / v2 共用查询参数；前端历史接口固定传 order=desc） */
export interface ChatHistoryQueryRequest {
  chatId: string;
  /** 页码从 1 起，缺省由 chat-history-service 内默认/环境变量决定 */
  page?: number;
  size?: number;
}

/** 与后端 ChatStreamQueryRequest 对齐 */
export interface ChatStreamQueryRequest {
  chatId: string;
  message: string;
  type?: string;
  /** 随消息一起发送的附件文件列表 */
  files?: File[];
}

/**
 * 与后端 ChatConversationListVo 对齐（v1 / v2 共用）。
 * v1 常见：title、updatedAt 为 null；v2 常有值。
 */
export interface ChatConversationListVo {
  conversationId: string;
  title?: string | null;
  type?: string | null;
  updatedAt?: string | null;
}

/** 会话列表在前端侧展示用 */
export interface ConversationListItem {
  id: string;
  title?: string | null;
}

/**
 * 与后端 ChatMessageHistoryVo 对齐（v1 / v2 共用）。
 * v1 常见：parts 为空、createdAt 为 null。
 */
export interface ChatMessageHistoryVo {
  id?: number;
  seq?: number;
  role?: string;
  content?: string | null;
  parts?: ChatMessagePartVo[];
  createdAt?: string | null;
}

/** 与后端 ChatMessagePartVo 对齐 */
export interface ChatMessagePartVo {
  partIndex?: number;
  partType?: string;
  contentText?: string | null;
  mediaUrl?: string | null;
  mimeType?: string | null;
  payload?: Record<string, unknown>;
}

/** 与后端 ChatMessageHistoryPageVo 对齐 */
export interface ChatMessageHistoryPageVo {
  records: ChatMessageHistoryVo[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 发送消息时，编辑器中待发送的本地文件 */
export interface PendingAttachment {
  id: string;
  file: File;
  localUrl: string;
}
