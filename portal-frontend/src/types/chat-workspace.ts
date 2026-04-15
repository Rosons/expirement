import type {
  ChatHistoryQueryRequest,
  ChatMessageHistoryPageVo,
  ChatStreamQueryRequest,
  ConversationListItem,
} from './chat';

export interface ChatWorkspaceApi {
  fetchConversations?: (options?: { skipErrorToast?: boolean }) => Promise<ConversationListItem[]>;
  fetchHistoryPage?: (query: ChatHistoryQueryRequest) => Promise<ChatMessageHistoryPageVo>;
  streamChatResponse: (
    query: ChatStreamQueryRequest,
    onChunk: (chunk: string) => void,
    signal?: AbortSignal,
  ) => Promise<void>;
}
