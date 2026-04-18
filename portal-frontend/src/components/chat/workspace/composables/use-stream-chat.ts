import { nextTick, ref, type Ref } from 'vue';
import type { ConversationListItem, PendingAttachment, UiChatMessage } from '../../../../types/chat';
import type { ChatWorkspaceApi } from '../../../../types/chat-workspace';
import {
  appendAssistantChunk,
  buildUiMessage,
  buildUserMessageWithParts,
  createChatId,
  finalizeAssistantMessage,
} from '../helpers/chat-workspace-helpers';

export type SendSource = 'user' | 'resend' | 'initial';

export type SendLifecyclePayload = {
  chatId: string;
  message: string;
  source: SendSource;
};

export type SendResultPayload = SendLifecyclePayload & {
  success: boolean;
  response: string;
};

type UseStreamChatOptions = {
  chatApi: ChatWorkspaceApi;
  currentChatId: Ref<string>;
  messages: Ref<UiChatMessage[]>;
  conversations: Ref<ConversationListItem[]>;
  draft: Ref<string>;
  resetUnreadState: () => void;
  scrollToBottom: (options?: { immediate?: boolean; retryCount?: number; force?: boolean }) => void;
  adjustComposerHeight: () => void;
  focusComposer: () => void;
  onBeforeSend: (payload: SendLifecyclePayload) => void;
  onAfterSend: (payload: SendResultPayload) => void;
};

export function useStreamChat(options: UseStreamChatOptions) {
  const isSending = ref(false);
  const requestStatus = ref('');
  const manualStopRequested = ref(false);
  const abortController = ref<AbortController | null>(null);

  async function sendMessageContent(
    rawContent: string,
    source: SendSource = 'user',
    files: PendingAttachment[] = [],
  ): Promise<void> {
    const userInput = rawContent.trim();
    const hasFiles = files && files.length > 0;
    if ((!userInput && !hasFiles) || isSending.value) {
      return;
    }

    const chatId = options.currentChatId.value || createChatId();
    if (!options.currentChatId.value) {
      options.currentChatId.value = chatId;
    }
    options.onBeforeSend({ chatId, message: userInput, source });

    const userMessage = hasFiles ? buildUserMessageWithParts(userInput, files) : buildUiMessage('user', userInput);
    const assistantMessage = buildUiMessage('assistant', '', true);
    options.messages.value.push(userMessage, assistantMessage);
    options.draft.value = '';
    isSending.value = true;
    manualStopRequested.value = false;
    requestStatus.value = hasFiles ? '正在发送...' : '请求已发送，等待 AI 响应...';
    options.resetUnreadState();
    options.scrollToBottom({ immediate: true, retryCount: 3, force: true });
    nextTick(options.adjustComposerHeight);

    const controller = new AbortController();
    abortController.value = controller;

    try {
      // 文件随聊天消息一起发送（由后端统一处理存储）
      await options.chatApi.streamChatResponse(
        { chatId, message: userInput, files: hasFiles ? files.map((f) => f.file) : undefined },
        (chunk) => {
          appendAssistantChunk(options.messages.value, assistantMessage.id, chunk);
          requestStatus.value = 'AI 正在生成中...';
          options.scrollToBottom({ immediate: true });
        },
        controller.signal,
      );
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        requestStatus.value = manualStopRequested.value ? '已停止生成' : '请求已取消';
      } else {
        requestStatus.value = '';
      }
    } finally {
      const hasVisibleContent = finalizeAssistantMessage(
        options.messages.value,
        assistantMessage.id,
        manualStopRequested.value
          ? '_已手动停止本次生成。_'
          : '_未收到可展示内容，请检查后端流格式（建议使用 SSE `data: ...`）。_',
      );
      if (!manualStopRequested.value && hasVisibleContent) {
        requestStatus.value = '响应完成';
      }
      const finalAssistantMessage = options.messages.value.find((item) => item.id === assistantMessage.id);
      options.onAfterSend({
        chatId,
        message: userInput,
        source,
        success: hasVisibleContent,
        response: finalAssistantMessage?.content ?? '',
      });
      isSending.value = false;
      abortController.value = null;

      if (!options.conversations.value.some((item) => item.id === options.currentChatId.value)) {
        options.conversations.value.unshift({ id: options.currentChatId.value });
      }

      if (options.chatApi.fetchConversations) {
        try {
          const latest = await options.chatApi.fetchConversations({ skipErrorToast: true });
          if (latest.length) {
            options.conversations.value = latest;
          }
        } catch {
          /* 发送后静默刷新会话列表，失败不打断主流程；也不重复 Toast */
        }
      }

      options.scrollToBottom({ immediate: true, retryCount: 4 });
      options.focusComposer();
    }
  }

  function stopGenerating(): void {
    manualStopRequested.value = true;
    requestStatus.value = '正在停止生成...';
    abortController.value?.abort();
  }

  return {
    isSending,
    requestStatus,
    sendMessageContent,
    stopGenerating,
  };
}
