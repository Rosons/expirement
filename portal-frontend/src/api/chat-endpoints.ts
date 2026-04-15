import { API_BASE_URL, CHAT_API_VERSION } from './env';

/**
 * 与后端 ChatV1Controller / ChatV2Controller 的 `/v1/ai/chat`、`/v2/ai/chat` 一致。
 * 开发代理见 `vite.config.ts` 的 `/v1/ai`、`/v2/ai`。
 */
export function getChatApiRootUrl(): string {
  return `${API_BASE_URL}/${CHAT_API_VERSION}/ai/chat`;
}

export function getChatHistoryUrl(): string {
  return `${getChatApiRootUrl()}/history`;
}

export function getChatConversationsUrl(): string {
  return `${getChatApiRootUrl()}/conversations`;
}

/** 流式对话：GET 根路径，查询参数 chatId、message */
export function getChatStreamUrl(): string {
  return getChatApiRootUrl();
}

export function getGameChatStreamUrl(): string {
  return `${API_BASE_URL}/ai/game/chat`;
}
