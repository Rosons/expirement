import {
  getChatConversationsUrl,
  getChatHistoryUrl,
  getCustomerChatHistoryUrl,
  getGameChatHistoryUrl,
  getKnowledgeChatConversationsUrl,
  getKnowledgeChatHistoryUrl,
} from '../../api/chat-endpoints';
import { CHAT_HISTORY_DEFAULT_PAGE, CHAT_HISTORY_PAGE_SIZE, getChatApiVersion } from '../../api/env';
import type {
  ChatConversationListVo,
  ChatHistoryQueryRequest,
  ChatMessageHistoryPageVo,
  ConversationListItem,
  UiChatMessage,
} from '../../types/chat';
import { apiClient } from '../http/http-client';
import { mapHistoryRecordsToUi } from './chat-message-mapper';

/** 与 .env 一致，供页面分页/上滑加载使用 */
export { CHAT_HISTORY_DEFAULT_PAGE, CHAT_HISTORY_PAGE_SIZE, getChatApiVersion };

const HISTORY_PAGE = CHAT_HISTORY_DEFAULT_PAGE;
const HISTORY_SIZE = CHAT_HISTORY_PAGE_SIZE;

/**
 * 拉取一页历史（原始分页对象，用于上滑加载等）。
 * 固定 order=desc：第 1 页为最近消息，与聊天界面自上而下「旧→新」展示一致。
 */
export async function fetchChatHistoryPage(query: ChatHistoryQueryRequest): Promise<ChatMessageHistoryPageVo> {
  const page = query.page ?? HISTORY_PAGE;
  const size = query.size ?? HISTORY_SIZE;
  const { data } = await apiClient.get<ChatMessageHistoryPageVo>(getChatHistoryUrl(), {
    params: {
      chatId: query.chatId,
      page,
      size,
      order: 'desc',
    },
  });
  return data;
}

export async function fetchConversations(options?: {
  skipErrorToast?: boolean;
  type?: string;
}): Promise<ConversationListItem[]> {
  const { data: list } = await apiClient.get<ChatConversationListVo[]>(getChatConversationsUrl(), {
    params: {
      type: options?.type,
    },
    skipGlobalErrorToast: options?.skipErrorToast === true,
  });
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map((row) => ({
    id: row.conversationId,
    title: row.title ?? undefined,
  }));
}

export async function fetchKnowledgeChatHistoryPage(query: ChatHistoryQueryRequest): Promise<ChatMessageHistoryPageVo> {
  const page = query.page ?? HISTORY_PAGE;
  const size = query.size ?? HISTORY_SIZE;
  const { data } = await apiClient.get<ChatMessageHistoryPageVo>(getKnowledgeChatHistoryUrl(), {
    params: {
      chatId: query.chatId,
      page,
      size,
      order: 'desc',
    },
  });
  return data;
}

export async function fetchCustomerChatHistoryPage(query: ChatHistoryQueryRequest): Promise<ChatMessageHistoryPageVo> {
  const page = query.page ?? HISTORY_PAGE;
  const size = query.size ?? HISTORY_SIZE;
  const { data } = await apiClient.get<ChatMessageHistoryPageVo>(getCustomerChatHistoryUrl(), {
    params: {
      chatId: query.chatId,
      page,
      size,
      order: 'desc',
    },
  });
  return data;
}

export async function fetchGameChatHistoryPage(query: ChatHistoryQueryRequest): Promise<ChatMessageHistoryPageVo> {
  const page = query.page ?? HISTORY_PAGE;
  const size = query.size ?? HISTORY_SIZE;
  const { data } = await apiClient.get<ChatMessageHistoryPageVo>(getGameChatHistoryUrl(), {
    params: {
      chatId: query.chatId,
      page,
      size,
      order: 'desc',
    },
  });
  return data;
}

export async function fetchKnowledgeConversations(options?: {
  skipErrorToast?: boolean;
  type?: string;
}): Promise<ConversationListItem[]> {
  const { data: list } = await apiClient.get<ChatConversationListVo[]>(getKnowledgeChatConversationsUrl(), {
    params: {
      type: options?.type,
    },
    skipGlobalErrorToast: options?.skipErrorToast === true,
  });
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map((row) => ({
    id: row.conversationId,
    title: row.title ?? undefined,
  }));
}

/**
 * 仅拉取一页并映射为 UI消息（默认页/页大小由环境变量决定）。
 */
export async function fetchHistory(query: ChatHistoryQueryRequest): Promise<UiChatMessage[]> {
  const page = await fetchChatHistoryPage(query);
  return mapHistoryRecordsToUi(page.records, query.chatId);
}
