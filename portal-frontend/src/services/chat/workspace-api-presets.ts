import type { ChatWorkspaceApi } from '../../types/chat-workspace';
import { fetchChatHistoryPage, fetchConversations, streamChatResponse } from './chat-service';
import { streamGameChatResponse } from './game-chat-service';

export const generalChatWorkspaceApi: ChatWorkspaceApi = {
  fetchConversations,
  fetchHistoryPage: fetchChatHistoryPage,
  streamChatResponse,
};

export const gameChatWorkspaceApi: ChatWorkspaceApi = {
  streamChatResponse: streamGameChatResponse,
};
