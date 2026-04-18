import {
  getChatFileDeleteUrl,
  getChatFileDownloadUrl,
  getChatFilesListUrl,
  getChatFileUploadUrl,
} from '../../api/chat-endpoints';
import type { ChatFileListItem, ChatFileUploadResult, UploadChatFileRequest } from '../../types/chat-file';
import { apiClient } from '../http/http-client';

function normalizeListItem(row: unknown): ChatFileListItem | null {
  if (row == null || typeof row !== 'object') {
    return null;
  }
  const o = row as Record<string, unknown>;
  const fileId = typeof o.fileId === 'string' ? o.fileId : '';
  const conversationId = typeof o.conversationId === 'string' ? o.conversationId : '';
  const originalFilename = typeof o.originalFilename === 'string' ? o.originalFilename : '';
  if (!fileId || !originalFilename) {
    return null;
  }
  const fileSizeRaw = o.fileSize;
  const fileSize =
    typeof fileSizeRaw === 'number'
      ? fileSizeRaw
      : typeof fileSizeRaw === 'string'
        ? Number(fileSizeRaw)
        : 0;
  return {
    fileId,
    conversationId,
    originalFilename,
    contentType: typeof o.contentType === 'string' ? o.contentType : null,
    fileSize: Number.isFinite(fileSize) ? fileSize : 0,
    createdAt: typeof o.createdAt === 'string' ? o.createdAt : null,
  };
}

export async function fetchChatFilesByConversation(conversationId: string): Promise<ChatFileListItem[]> {
  const trimmed = conversationId.trim();
  if (!trimmed) {
    return [];
  }
  const { data } = await apiClient.get<unknown>(getChatFilesListUrl(), {
    params: { conversationId: trimmed },
  });
  if (!Array.isArray(data)) {
    return [];
  }
  return data.map(normalizeListItem).filter((x): x is ChatFileListItem => x != null);
}

export async function uploadChatFile(request: UploadChatFileRequest): Promise<ChatFileUploadResult> {
  const formData = new FormData();
  formData.append('conversationId', request.conversationId);
  if (request.conversationType?.trim()) {
    formData.append('type', request.conversationType.trim());
  }
  formData.append('file', request.file);

  const { data } = await apiClient.post<ChatFileUploadResult>(getChatFileUploadUrl(), formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return data;
}

export async function deleteChatFile(fileId: string, conversationId: string): Promise<void> {
  await apiClient.delete(getChatFileDeleteUrl(fileId), {
    params: { conversationId: conversationId.trim() },
  });
}

/** 文本类预览：仅拉取字符串，避免先整包 blob 再解码 */
export async function fetchChatFileText(fileId: string): Promise<string> {
  const response = await fetch(getChatFileDownloadUrl(fileId), { method: 'GET' });
  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(`读取文件失败：${response.status}${message ? `，${message.slice(0, 120)}` : ''}`);
  }
  return await response.text();
}
