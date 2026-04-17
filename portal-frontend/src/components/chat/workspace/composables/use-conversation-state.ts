import { nextTick, ref, type Ref } from 'vue';
import { CHAT_HISTORY_PAGE_SIZE, mapHistoryRecordsToUi } from '../../../../services/chat';
import type { ConversationListItem, UiChatMessage } from '../../../../types/chat';
import type { ChatWorkspaceApi } from '../../../../types/chat-workspace';
import { buildUiMessage, createChatId } from '../helpers/chat-workspace-helpers';

type UseConversationStateOptions = {
  chatApi: ChatWorkspaceApi;
  chatBodyRef: Ref<HTMLElement | null>;
  resetUnreadState: () => void;
  scrollToBottom: (options?: { immediate?: boolean; retryCount?: number; force?: boolean }) => void;
  /** 固定会话：不拉会话列表，启动时只加载该会话历史 */
  fixedSessionChatId?: string;
  /** 历史接口成功且条数为 0 时插入本地助手消息（不请求后端） */
  localWelcomeAssistantMessage?: string;
};

export function useConversationState(options: UseConversationStateOptions) {
  const conversations = ref<ConversationListItem[]>([]);
  const currentChatId = ref('');
  const messages = ref<UiChatMessage[]>([]);
  const isLoadingHistory = ref(false);
  const isLoadingConversations = ref(false);
  const historyLastLoadedPage = ref(1);
  const historyTotalPages = ref(1);
  const hasMoreOlder = ref(false);
  const isLoadingOlder = ref(false);

  async function loadConversations(): Promise<void> {
    if (!options.chatApi.fetchConversations) {
      conversations.value = [];
      return;
    }
    isLoadingConversations.value = true;
    try {
      const list = await options.chatApi.fetchConversations();
      conversations.value = list;
    } catch {
      /* 错误文案由全局 Toast 统一展示 */
    } finally {
      isLoadingConversations.value = false;
    }
  }

  async function loadOlderMessages(): Promise<void> {
    if (!options.chatApi.fetchHistoryPage) {
      hasMoreOlder.value = false;
      return;
    }
    const chatId = currentChatId.value;
    if (!chatId || isLoadingOlder.value || !hasMoreOlder.value || isLoadingHistory.value) {
      return;
    }
    const element = options.chatBodyRef.value;
    if (!element) {
      return;
    }
    isLoadingOlder.value = true;
    const prevScrollHeight = element.scrollHeight;
    const prevScrollTop = element.scrollTop;
    try {
      const nextPage = historyLastLoadedPage.value + 1;
      if (nextPage > historyTotalPages.value) {
        hasMoreOlder.value = false;
        return;
      }
      const pageVo = await options.chatApi.fetchHistoryPage({
        chatId,
        page: nextPage,
        size: CHAT_HISTORY_PAGE_SIZE,
      });
      const older = mapHistoryRecordsToUi(pageVo.records, chatId);
      messages.value = [...older, ...messages.value];
      historyLastLoadedPage.value = nextPage;
      hasMoreOlder.value = nextPage < historyTotalPages.value;
      await nextTick();
      const newScrollHeight = element.scrollHeight;
      element.scrollTop = newScrollHeight - prevScrollHeight + prevScrollTop;
    } catch {
      /* 全局 Toast */
    } finally {
      isLoadingOlder.value = false;
    }
  }

  async function loadHistory(chatId: string): Promise<boolean> {
    if (!options.chatApi.fetchHistoryPage) {
      currentChatId.value = chatId;
      messages.value = [];
      hasMoreOlder.value = false;
      return true;
    }
    currentChatId.value = chatId;
    isLoadingHistory.value = true;
    hasMoreOlder.value = false;
    isLoadingOlder.value = false;
    historyLastLoadedPage.value = 1;
    historyTotalPages.value = 1;
    try {
      const first = await options.chatApi.fetchHistoryPage({ chatId, page: 1, size: CHAT_HISTORY_PAGE_SIZE });
      if (first.total === 0) {
        messages.value = [];
        return true;
      }
      historyTotalPages.value = Math.max(1, first.pages);
      messages.value = mapHistoryRecordsToUi(first.records, chatId);
      historyLastLoadedPage.value = 1;
      hasMoreOlder.value = historyTotalPages.value > 1;
      return true;
    } catch {
      messages.value = [];
      return false;
    } finally {
      isLoadingHistory.value = false;
      options.resetUnreadState();
      options.scrollToBottom({ immediate: true, retryCount: 4, force: true });
    }
  }

  async function bootstrapChatState(): Promise<void> {
    const fixedId = options.fixedSessionChatId?.trim();
    if (fixedId) {
      conversations.value = [{ id: fixedId }];
      const ok = await loadHistory(fixedId);
      const welcome = options.localWelcomeAssistantMessage?.trim();
      if (ok && messages.value.length === 0 && welcome) {
        messages.value.push(buildUiMessage('assistant', welcome, false));
        options.resetUnreadState();
        options.scrollToBottom({ immediate: true, retryCount: 4, force: true });
      }
      return;
    }

    if (!options.chatApi.fetchConversations || !options.chatApi.fetchHistoryPage) {
      const chatId = createChatId();
      currentChatId.value = chatId;
      conversations.value = [{ id: chatId }];
      messages.value = [];
      hasMoreOlder.value = false;
      historyLastLoadedPage.value = 1;
      historyTotalPages.value = 1;
      return;
    }
    await loadConversations();

    let defaultChatId: string;
    if (conversations.value.length) {
      defaultChatId = conversations.value[0].id;
    } else {
      defaultChatId = createChatId();
      conversations.value = [{ id: defaultChatId }];
    }
    await loadHistory(defaultChatId);
  }

  function startNewConversationBase(): string {
    const chatId = createChatId();
    currentChatId.value = chatId;
    messages.value = [];
    hasMoreOlder.value = false;
    historyLastLoadedPage.value = 1;
    historyTotalPages.value = 1;
    if (!conversations.value.some((item) => item.id === chatId)) {
      conversations.value = [{ id: chatId }, ...conversations.value];
    }
    return chatId;
  }

  return {
    conversations,
    currentChatId,
    messages,
    isLoadingHistory,
    isLoadingConversations,
    hasMoreOlder,
    isLoadingOlder,
    loadOlderMessages,
    loadHistory,
    bootstrapChatState,
    startNewConversationBase,
  };
}
