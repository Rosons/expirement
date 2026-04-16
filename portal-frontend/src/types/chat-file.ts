export interface ChatFileUploadResult {
  fileId: string;
  conversationId: string;
  originalFilename: string;
  contentType?: string | null;
  fileSize: number;
  downloadUrl: string;
}

export interface UploadChatFileRequest {
  conversationId: string;
  file: File;
  /** 与后端 `chat_conversation.type` 一致；传入则上传时先登记会话（不必先发聊天消息） */
  conversationType?: string;
}

/** 与后端 ChatFileListVo JSON 对齐 */
export interface ChatFileListItem {
  fileId: string;
  conversationId: string;
  originalFilename: string;
  contentType?: string | null;
  fileSize: number;
  createdAt?: string | null;
}
