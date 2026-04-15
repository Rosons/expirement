<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import ChatMessageBubble from './ChatMessageBubble.vue';
import { CHAT_HISTORY_PAGE_SIZE, mapHistoryRecordsToUi } from '../../services/chat/chat-service';
import type { ConversationListItem, UiChatMessage } from '../../types/chat';
import type { ChatWorkspaceApi } from '../../types/chat-workspace';

type SendSource = 'user' | 'resend' | 'initial';
type SendLifecyclePayload = {
  chatId: string;
  message: string;
  source: SendSource;
};
type SendResultPayload = SendLifecyclePayload & {
  success: boolean;
  response: string;
};

const props = withDefaults(
  defineProps<{
    chatApi: ChatWorkspaceApi;
    showConversationSidebar?: boolean;
    showNewConversationButton?: boolean;
    showMessageCopyAction?: boolean;
    showMessageResendAction?: boolean;
    initialMessage?: string;
    emptyTitle?: string;
    emptyDescription?: string;
    composerPlaceholder?: string;
  }>(),
  {
    showConversationSidebar: true,
    showNewConversationButton: true,
    showMessageCopyAction: true,
    showMessageResendAction: true,
    initialMessage: '',
    emptyTitle: '开始聊天',
    emptyDescription: '在下方输入你的问题，按 Enter 发送。',
    composerPlaceholder: '输入你的问题，Enter 发送，Shift+Enter 换行',
  },
);
const emit = defineEmits<{
  beforeSend: [payload: SendLifecyclePayload];
  afterSend: [payload: SendResultPayload];
  conversationChange: [chatId: string];
}>();

const conversations = ref<ConversationListItem[]>([]);
const currentChatId = ref('');
const messages = ref<UiChatMessage[]>([]);
const draft = ref('');
const isSending = ref(false);
const isLoadingHistory = ref(false);
const isLoadingConversations = ref(false);
const chatBodyRef = ref<HTMLElement | null>(null);
const composerInputRef = ref<HTMLTextAreaElement | null>(null);
const abortController = ref<AbortController | null>(null);
const requestStatus = ref('');
const manualStopRequested = ref(false);
const shouldAutoScroll = ref(true);
const hasUnseenNewMessages = ref(false);
const consumedInitialMessage = ref(false);
/**
 * order=desc：第 1 页为最近消息；已加载的最大页码（上滑加载下一页 = +1，更旧）。
 */
const historyLastLoadedPage = ref(1);
const historyTotalPages = ref(1);
const hasMoreOlder = ref(false);
const isLoadingOlder = ref(false);

const canSend = computed(() => !isSending.value && draft.value.trim().length > 0);
const totalMessageCount = computed(() => messages.value.length);
const draftLength = computed(() => draft.value.trim().length);
const shortChatId = computed(() => {
  const chatId = currentChatId.value;
  if (!chatId) {
    return '未初始化';
  }
  if (chatId.length <= 26) {
    return chatId;
  }
  return `${chatId.slice(0, 12)}...${chatId.slice(-8)}`;
});

function focusComposer(): void {
  nextTick(() => {
    composerInputRef.value?.focus();
  });
}

function adjustComposerHeight(): void {
  const input = composerInputRef.value;
  if (!input) {
    return;
  }

  input.style.height = 'auto';
  const nextHeight = Math.min(Math.max(input.scrollHeight, 56), 180);
  input.style.height = `${nextHeight}px`;
  input.style.overflowY = input.scrollHeight > 180 ? 'auto' : 'hidden';
}

function createChatId(): string {
  return `chat-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function buildUiMessage(role: UiChatMessage['role'], content = '', streaming = false): UiChatMessage {
  const messageId =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `msg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;

  return {
    id: messageId,
    role,
    content,
    createdAt: Date.now(),
    streaming,
  };
}

function isNearBottom(threshold = 72): boolean {
  if (!chatBodyRef.value) {
    return true;
  }

  const { scrollTop, clientHeight, scrollHeight } = chatBodyRef.value;
  return scrollTop + clientHeight >= scrollHeight - threshold;
}

function isNearTop(threshold = 120): boolean {
  if (!chatBodyRef.value) {
    return false;
  }
  return chatBodyRef.value.scrollTop <= threshold;
}

function handleChatScroll(): void {
  const nearBottom = isNearBottom();
  shouldAutoScroll.value = nearBottom;
  if (nearBottom) {
    hasUnseenNewMessages.value = false;
  }
  if (isNearTop() && hasMoreOlder.value && !isLoadingOlder.value && !isLoadingHistory.value) {
    void loadOlderMessages();
  }
}

function scrollToBottom(options?: { immediate?: boolean; retryCount?: number; force?: boolean }): void {
  if (!chatBodyRef.value) {
    return;
  }

  const immediate = options?.immediate ?? false;
  const retryCount = options?.retryCount ?? 2;
  const force = options?.force ?? false;

  if (!force && !shouldAutoScroll.value) {
    return;
  }

  nextTick(() => {
    const behavior: ScrollBehavior = immediate ? 'auto' : 'smooth';
    chatBodyRef.value?.scrollTo({ top: chatBodyRef.value.scrollHeight, behavior });

    requestAnimationFrame(() => {
      chatBodyRef.value?.scrollTo({ top: chatBodyRef.value.scrollHeight, behavior: 'auto' });
    });

    if (retryCount > 0) {
      setTimeout(() => {
        scrollToBottom({ immediate: true, retryCount: retryCount - 1, force });
      }, 80);
    }
  });
}

function scrollToLatestMessage(): void {
  shouldAutoScroll.value = true;
  hasUnseenNewMessages.value = false;
  scrollToBottom({ immediate: false, retryCount: 4, force: true });
}

function shortenChatId(chatId: string): string {
  if (chatId.length <= 30) {
    return chatId;
  }
  return `${chatId.slice(0, 12)}...${chatId.slice(-8)}`;
}

function getConversationTitle(item: ConversationListItem, index: number): string {
  if (item.id === currentChatId.value) {
    return '当前会话';
  }
  const t = item.title?.trim();
  if (t) {
    return t;
  }
  return `会话 ${String(index + 1).padStart(2, '0')}`;
}

function getConversationSubtitle(item: ConversationListItem): string {
  return shortenChatId(item.id);
}

function appendAssistantChunk(messageId: string, chunk: string): void {
  const targetIndex = messages.value.findIndex((item) => item.id === messageId);
  if (targetIndex < 0) {
    return;
  }

  const targetMessage = messages.value[targetIndex];
  messages.value[targetIndex] = {
    ...targetMessage,
    content: `${targetMessage.content}${chunk}`,
  };
}

function finalizeAssistantMessage(messageId: string, fallbackContent?: string): boolean {
  const targetIndex = messages.value.findIndex((item) => item.id === messageId);
  if (targetIndex < 0) {
    return false;
  }

  const targetMessage = messages.value[targetIndex];
  const finalContent = targetMessage.content || fallbackContent || '';
  messages.value[targetIndex] = {
    ...targetMessage,
    streaming: false,
    content: finalContent,
  };

  return finalContent.trim().length > 0;
}

async function loadConversations(): Promise<void> {
  if (!props.chatApi.fetchConversations) {
    conversations.value = [];
    return;
  }
  isLoadingConversations.value = true;
  try {
    const list = await props.chatApi.fetchConversations();
    conversations.value = list;
  } catch {
    /* 错误文案由全局 Toast 统一展示 */
  } finally {
    isLoadingConversations.value = false;
  }
}

async function loadOlderMessages(): Promise<void> {
  if (!props.chatApi.fetchHistoryPage) {
    hasMoreOlder.value = false;
    return;
  }
  const chatId = currentChatId.value;
  if (!chatId || isLoadingOlder.value || !hasMoreOlder.value || isLoadingHistory.value) {
    return;
  }
  const el = chatBodyRef.value;
  if (!el) {
    return;
  }
  isLoadingOlder.value = true;
  const prevScrollHeight = el.scrollHeight;
  const prevScrollTop = el.scrollTop;
  try {
    const nextPage = historyLastLoadedPage.value + 1;
    if (nextPage > historyTotalPages.value) {
      hasMoreOlder.value = false;
      return;
    }
    const pageVo = await props.chatApi.fetchHistoryPage({
      chatId,
      page: nextPage,
      size: CHAT_HISTORY_PAGE_SIZE,
    });
    const older = mapHistoryRecordsToUi(pageVo.records, chatId);
    messages.value = [...older, ...messages.value];
    historyLastLoadedPage.value = nextPage;
    hasMoreOlder.value = nextPage < historyTotalPages.value;
    await nextTick();
    const newScrollHeight = el.scrollHeight;
    el.scrollTop = newScrollHeight - prevScrollHeight + prevScrollTop;
  } catch {
    /* 全局 Toast */
  } finally {
    isLoadingOlder.value = false;
  }
}

async function loadHistory(chatId: string): Promise<void> {
  if (!props.chatApi.fetchHistoryPage) {
    currentChatId.value = chatId;
    messages.value = [];
    hasMoreOlder.value = false;
    return;
  }
  currentChatId.value = chatId;
  isLoadingHistory.value = true;
  hasMoreOlder.value = false;
  isLoadingOlder.value = false;
  historyLastLoadedPage.value = 1;
  historyTotalPages.value = 1;
  try {
    const size = CHAT_HISTORY_PAGE_SIZE;
    // fetchChatHistoryPage 固定 order=desc：第 1 页即最近一段
    const first = await props.chatApi.fetchHistoryPage({ chatId, page: 1, size });
    if (first.total === 0) {
      messages.value = [];
      return;
    }
    historyTotalPages.value = Math.max(1, first.pages);
    messages.value = mapHistoryRecordsToUi(first.records, chatId);
    historyLastLoadedPage.value = 1;
    hasMoreOlder.value = historyTotalPages.value > 1;
  } catch {
    messages.value = [];
  } finally {
    isLoadingHistory.value = false;
    shouldAutoScroll.value = true;
    hasUnseenNewMessages.value = false;
    scrollToBottom({ immediate: true, retryCount: 4, force: true });
  }
}

async function bootstrapChatState(): Promise<void> {
  if (!props.chatApi.fetchConversations || !props.chatApi.fetchHistoryPage) {
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

function startNewConversation(): void {
  const chatId = createChatId();
  currentChatId.value = chatId;
  messages.value = [];
  hasMoreOlder.value = false;
  historyLastLoadedPage.value = 1;
  historyTotalPages.value = 1;
  draft.value = '';
  requestStatus.value = '';
  shouldAutoScroll.value = true;
  hasUnseenNewMessages.value = false;
  if (!conversations.value.some((c) => c.id === chatId)) {
    conversations.value = [{ id: chatId }, ...conversations.value];
  }
  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
}

async function sendMessageContent(rawContent: string, source: SendSource = 'user'): Promise<void> {
  const userInput = rawContent.trim();
  if (!userInput || isSending.value) {
    return;
  }

  const chatId = currentChatId.value || createChatId();
  if (!currentChatId.value) {
    currentChatId.value = chatId;
  }
  emit('beforeSend', { chatId, message: userInput, source });

  const userMessage = buildUiMessage('user', userInput);
  const assistantMessage = buildUiMessage('assistant', '', true);
  messages.value.push(userMessage, assistantMessage);
  draft.value = '';
  isSending.value = true;
  manualStopRequested.value = false;
  requestStatus.value = '请求已发送，等待 AI 响应...';
  shouldAutoScroll.value = true;
  hasUnseenNewMessages.value = false;
  scrollToBottom({ immediate: true, retryCount: 3, force: true });
  nextTick(adjustComposerHeight);

  const controller = new AbortController();
  abortController.value = controller;

  try {
    await props.chatApi.streamChatResponse(
      { chatId, message: userInput },
      (chunk) => {
        appendAssistantChunk(assistantMessage.id, chunk);
        requestStatus.value = 'AI 正在生成中...';
        scrollToBottom({ immediate: true });
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
      assistantMessage.id,
      manualStopRequested.value
        ? '_已手动停止本次生成。_'
        : '_未收到可展示内容，请检查后端流格式（建议使用 SSE `data: ...`）。_',
    );
    if (!manualStopRequested.value && hasVisibleContent) {
      requestStatus.value = '响应完成';
    }
    const finalAssistantMessage = messages.value.find((item) => item.id === assistantMessage.id);
    emit('afterSend', {
      chatId,
      message: userInput,
      source,
      success: hasVisibleContent,
      response: finalAssistantMessage?.content ?? '',
    });
    isSending.value = false;
    abortController.value = null;

    if (!conversations.value.some((c) => c.id === currentChatId.value)) {
      conversations.value.unshift({ id: currentChatId.value });
    }

    if (props.chatApi.fetchConversations) {
      try {
        const latest = await props.chatApi.fetchConversations({ skipErrorToast: true });
        if (latest.length) {
          conversations.value = latest;
        }
      } catch {
        /* 发送后静默刷新会话列表，失败不打断主流程；也不重复 Toast */
      }
    }

    scrollToBottom({ immediate: true, retryCount: 4 });
    focusComposer();
  }
}

async function handleSendMessage(): Promise<void> {
  const userInput = draft.value.trim();
  if (!userInput || isSending.value) {
    return;
  }
  draft.value = '';
  nextTick(adjustComposerHeight);
  await sendMessageContent(userInput, 'user');
}

async function handleResendMessage(content: string): Promise<void> {
  if (isSending.value) {
    return;
  }
  await sendMessageContent(content, 'resend');
}

function stopGenerating(): void {
  manualStopRequested.value = true;
  requestStatus.value = '正在停止生成...';
  abortController.value?.abort();
}

function handleEnter(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    void handleSendMessage();
  }
}

onMounted(() => {
  void (async () => {
    await bootstrapChatState();
    const entryMessage = props.initialMessage.trim();
    if (!consumedInitialMessage.value && entryMessage) {
      consumedInitialMessage.value = true;
      await sendMessageContent(entryMessage, 'initial');
    }
  })();

  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
});

watch(draft, () => {
  nextTick(adjustComposerHeight);
});

watch(
  currentChatId,
  (chatId) => {
    if (chatId) {
      emit('conversationChange', chatId);
    }
  },
  { flush: 'post' },
);

watch(
  () => {
    const last = messages.value.at(-1);
    return `${messages.value.length}:${last?.id ?? ''}:${last?.content.length ?? 0}`;
  },
  () => {
    if (!shouldAutoScroll.value) {
      hasUnseenNewMessages.value = true;
      return;
    }
    scrollToBottom({ immediate: true, retryCount: 2 });
  },
);
</script>

<template>
  <section class="chat-layout" :class="{ 'chat-layout--single': !showConversationSidebar }">
    <aside v-if="showConversationSidebar" class="conversation-sidebar">
      <div class="sidebar-head">
        <h2>会话</h2>
        <button v-if="showNewConversationButton" type="button" class="new-chat-button" @click="startNewConversation">
          新会话
        </button>
      </div>
      <div v-if="isLoadingConversations" class="sidebar-placeholder">会话加载中...</div>
      <ul v-else-if="conversations.length" class="conversation-list">
        <li v-for="(item, index) in conversations" :key="item.id">
          <button
            type="button"
            class="conversation-item"
            :class="{ 'conversation-item--active': item.id === currentChatId }"
            @click="loadHistory(item.id)"
          >
            <span class="conversation-item__title">{{ getConversationTitle(item, index) }}</span>
            <span class="conversation-item__subtitle" :title="item.id">{{ getConversationSubtitle(item) }}</span>
          </button>
        </li>
      </ul>
      <div v-else class="sidebar-placeholder">还没有会话，先新建一个吧。</div>
    </aside>

    <section class="chat-main">
      <div ref="chatBodyRef" class="message-list" @scroll.passive="handleChatScroll">
        <div v-if="isLoadingOlder" class="history-load-hint" aria-live="polite">加载更早消息中…</div>
        <div v-else-if="hasMoreOlder && messages.length > 0" class="history-load-hint history-load-hint--muted">
          上滑加载更早消息
        </div>
        <div v-if="isLoadingHistory" class="placeholder-card">历史消息加载中...</div>
        <div v-else-if="messages.length === 0" class="empty-state">
          <h3>{{ emptyTitle }}</h3>
          <p>{{ emptyDescription }}</p>
        </div>
        <ChatMessageBubble
          v-for="message in messages"
          :key="message.id"
          :message-id="message.id"
          :role="message.role"
          :content="message.content"
          :created-at="message.createdAt"
          :streaming="message.streaming"
          :enable-copy-action="showMessageCopyAction"
          :enable-resend-action="showMessageResendAction"
          @resend-message="handleResendMessage"
        />
      </div>

      <button
        v-if="!shouldAutoScroll && hasUnseenNewMessages"
        type="button"
        class="scroll-bottom-fab"
        @click="scrollToLatestMessage"
      >
        回到底部
      </button>

      <footer class="composer">
        <div class="composer-shell">
          <textarea
            ref="composerInputRef"
            v-model="draft"
            class="composer-input"
            :placeholder="composerPlaceholder"
            :disabled="isSending"
            @keydown="handleEnter"
            @input="adjustComposerHeight"
          />
          <div class="composer-actions">
            <div class="composer-meta">
              <span>当前会话：{{ shortChatId }}</span>
              <span>{{ totalMessageCount }} 条消息</span>
              <span v-if="draftLength > 0">输入中 {{ draftLength }} 字</span>
              <span v-if="requestStatus">{{ requestStatus }}</span>
            </div>
            <div class="action-buttons">
              <button v-if="isSending" type="button" class="ghost-button" @click="stopGenerating">停止</button>
              <button type="button" class="primary-button" :disabled="!canSend" @click="handleSendMessage">
                {{ isSending ? '生成中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </footer>
    </section>
  </section>
</template>

<style scoped>
.chat-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
}

.chat-layout--single {
  grid-template-columns: minmax(0, 1fr);
}

.conversation-sidebar,
.chat-main {
  min-height: 0;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(16px);
}

.conversation-sidebar {
  padding: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.sidebar-head h2 {
  margin: 0;
  font-size: 16px;
}

.new-chat-button {
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  padding: 8px 12px;
  background: rgba(239, 246, 255, 0.9);
  color: #1d4ed8;
  font-weight: 600;
  cursor: pointer;
}

.conversation-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.sidebar-placeholder {
  padding: 16px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.88);
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.conversation-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: #fff;
  color: #334155;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.conversation-item:hover {
  transform: translateY(-1px);
  border-color: rgba(96, 165, 250, 0.3);
}

.conversation-item--active {
  border-color: rgba(59, 130, 246, 0.36);
  background: rgba(239, 246, 255, 0.95);
}

.conversation-item__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.conversation-item__subtitle {
  max-width: 100%;
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-main {
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  padding: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.88)),
    rgba(255, 255, 255, 0.88);
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 22px 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  border-radius: 16px 16px 12px 12px;
  background: rgba(248, 250, 252, 0.36);
  scroll-padding-top: 22px;
}

.history-load-hint {
  flex-shrink: 0;
  text-align: center;
  padding: 4px 12px 0;
  font-size: 12px;
  color: #64748b;
}

.history-load-hint--muted {
  opacity: 0.8;
}

.placeholder-card,
.empty-state {
  margin: auto;
  width: min(100%, 560px);
  padding: 22px;
  text-align: center;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.88);
  color: #64748b;
}

.empty-state h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.empty-state p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.scroll-bottom-fab {
  position: absolute;
  right: 20px;
  bottom: 118px;
  z-index: 3;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 999px;
  padding: 9px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #1d4ed8;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 26px rgba(59, 130, 246, 0.12);
  cursor: pointer;
}

.composer {
  flex-shrink: 0;
  padding: 10px 0 0;
  background: transparent;
}

.composer-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.composer-input {
  width: 100%;
  min-height: 56px;
  max-height: 180px;
  resize: none;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #0f172a;
  background: #fff;
}

.composer-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.68);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.primary-button,
.ghost-button {
  border-radius: 10px;
  padding: 8px 14px;
  border: 1px solid transparent;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  color: #fff;
  background: linear-gradient(140deg, #2563eb, #1d4ed8);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-button {
  color: #334155;
  background: #fff;
  border-color: rgba(148, 163, 184, 0.42);
}

@media (max-width: 1024px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-layout--single {
    grid-template-columns: 1fr;
  }

  .conversation-sidebar {
    max-height: 220px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .message-list {
    padding: 18px 14px 12px;
  }

  .composer {
    padding: 10px 0 0;
  }

  .scroll-bottom-fab {
    right: 12px;
    bottom: 110px;
  }
}
</style>
