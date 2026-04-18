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
  return `${API_BASE_URL}/game/ai/chat`;
}

export function getGameChatHistoryUrl(): string {
  return `${API_BASE_URL}/game/ai/chat/history`;
}

/** {@link KnowledgeChatController}：`/knowledge/ai/chat` */
export function getKnowledgeChatApiRootUrl(): string {
  return `${API_BASE_URL}/knowledge/ai/chat`;
}

export function getKnowledgeChatHistoryUrl(): string {
  return `${getKnowledgeChatApiRootUrl()}/history`;
}

export function getKnowledgeChatConversationsUrl(): string {
  return `${getKnowledgeChatApiRootUrl()}/conversations`;
}

export function getKnowledgeChatStreamUrl(): string {
  return getKnowledgeChatApiRootUrl();
}

/** 后端 `CustomerController`：`/customer/ai/chat` */
export function getCustomerChatApiRootUrl(): string {
  return `${API_BASE_URL}/customer/ai/chat`;
}

export function getCustomerChatHistoryUrl(): string {
  return `${getCustomerChatApiRootUrl()}/history`;
}

/** 后端 `ChatFileController` 的 `@RequestMapping("/ai/files")` 根路径（不含尾斜杠） */
function chatFilesRootPath(): string {
  return `${API_BASE_URL}/ai/files`;
}

export function getChatFileUploadUrl(): string {
  return `${chatFilesRootPath()}/upload`;
}

/** GET：按会话列举，`?conversationId=` */
export function getChatFilesListUrl(): string {
  return chatFilesRootPath();
}

/** GET：下载（`attachment`，供「下载」按钮） */
export function getChatFileDownloadUrl(fileId: string): string {
  return `${chatFilesRootPath()}/download/${encodeURIComponent(fileId)}`;
}

/** GET：内联预览（`?inline=1`，`inline` + 真实 Content-Type，供 img/iframe） */
export function getChatFilePreviewUrl(fileId: string): string {
  return `${getChatFileDownloadUrl(fileId)}?inline=1`;
}

/** DELETE：按 id 删除（`/ai/files/{fileId}`，`conversationId` 由 axios params 传入） */
export function getChatFileDeleteUrl(fileId: string): string {
  return `${chatFilesRootPath()}/${encodeURIComponent(fileId)}`;
}
