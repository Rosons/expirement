import {
  CHAT_PRODUCT_TYPE_CUSTOMER,
  CHAT_PRODUCT_TYPE_GAME_CHAT,
  CHAT_PRODUCT_TYPE_GENERAL,
  CHAT_PRODUCT_TYPE_KNOWLEDGE,
} from '../../constants/chat-product-types';
import type { ChatWorkspaceApi } from '../../types/chat-workspace';
import {
  fetchChatHistoryPage,
  fetchConversations,
  fetchCustomerChatHistoryPage,
  fetchGameChatHistoryPage,
  fetchKnowledgeChatHistoryPage,
  fetchKnowledgeConversations,
} from './chat-history-service';
import {
  streamChatResponse,
  streamCustomerChatResponse,
  streamKnowledgeChatResponse,
} from './chat-stream-service';
import { streamGameChatResponse } from './game-chat-service';

export const generalChatWorkspaceApi: ChatWorkspaceApi = {
  fetchConversations: (options) =>
    fetchConversations({
      ...options,
      type: CHAT_PRODUCT_TYPE_GENERAL,
    }),
  fetchHistoryPage: fetchChatHistoryPage,
  streamChatResponse: (query, onChunk, signal) =>
    streamChatResponse(
      {
        ...query,
        type: CHAT_PRODUCT_TYPE_GENERAL,
      },
      onChunk,
      signal,
    ),
};

export const gameChatWorkspaceApi: ChatWorkspaceApi = {
  fetchHistoryPage: fetchGameChatHistoryPage,
  streamChatResponse: (query, onChunk, signal) =>
    streamGameChatResponse(
      {
        ...query,
        type: CHAT_PRODUCT_TYPE_GAME_CHAT,
      },
      onChunk,
      signal,
    ),
};

export const knowledgeChatWorkspaceApi: ChatWorkspaceApi = {
  fetchConversations: (options) =>
    fetchKnowledgeConversations({
      ...options,
      type: CHAT_PRODUCT_TYPE_KNOWLEDGE,
    }),
  fetchHistoryPage: fetchKnowledgeChatHistoryPage,
  streamChatResponse: (query, onChunk, signal) =>
    streamKnowledgeChatResponse(
      {
        ...query,
        type: CHAT_PRODUCT_TYPE_KNOWLEDGE,
      },
      onChunk,
      signal,
    ),
};

export const customerChatWorkspaceApi: ChatWorkspaceApi = {
  fetchHistoryPage: fetchCustomerChatHistoryPage,
  streamChatResponse: (query, onChunk, signal) =>
    streamCustomerChatResponse(
      {
        ...query,
        type: CHAT_PRODUCT_TYPE_CUSTOMER,
      },
      onChunk,
      signal,
    ),
};
