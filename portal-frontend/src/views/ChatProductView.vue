<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import ChatMessageBubble from '../components/chat/ChatMessageBubble.vue';
import { fetchConversations, fetchHistory, streamChatResponse } from '../services/chat-api';
import type { UiChatMessage } from '../types/chat';

const router = useRouter();

const conversations = ref<string[]>([]);
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

function handleChatScroll(): void {
  const nearBottom = isNearBottom();
  shouldAutoScroll.value = nearBottom;
  if (nearBottom) {
    hasUnseenNewMessages.value = false;
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

function getConversationTitle(chatId: string, index: number): string {
  if (chatId === currentChatId.value) {
    return '当前会话';
  }
  return `会话 ${String(index + 1).padStart(2, '0')}`;
}

function getConversationSubtitle(chatId: string): string {
  if (chatId === currentChatId.value) {
    return '正在查看';
  }
  return shortenChatId(chatId);
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
  isLoadingConversations.value = true;
  try {
    const list = await fetchConversations();
    conversations.value = list;
  } catch (error) {
    console.error(error);
  } finally {
    isLoadingConversations.value = false;
  }
}

async function loadHistory(chatId: string): Promise<void> {
  currentChatId.value = chatId;
  isLoadingHistory.value = true;
  try {
    messages.value = await fetchHistory(chatId);
  } catch (error) {
    messages.value = [
      buildUiMessage('system', `加载历史消息失败：${error instanceof Error ? error.message : '未知错误'}`),
    ];
  } finally {
    isLoadingHistory.value = false;
    shouldAutoScroll.value = true;
    hasUnseenNewMessages.value = false;
    scrollToBottom({ immediate: true, retryCount: 4, force: true });
  }
}

async function bootstrapChatState(): Promise<void> {
  await loadConversations();

  const defaultChatId = conversations.value[0] ?? createChatId();
  if (!conversations.value.length) {
    conversations.value = [defaultChatId];
  }
  await loadHistory(defaultChatId);
}

function startNewConversation(): void {
  const chatId = createChatId();
  currentChatId.value = chatId;
  messages.value = [];
  draft.value = '';
  requestStatus.value = '';
  shouldAutoScroll.value = true;
  hasUnseenNewMessages.value = false;
  if (!conversations.value.includes(chatId)) {
    conversations.value = [chatId, ...conversations.value];
  }
  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
}

async function sendMessageContent(rawContent: string): Promise<void> {
  const userInput = rawContent.trim();
  if (!userInput || isSending.value) {
    return;
  }

  if (!currentChatId.value) {
    currentChatId.value = createChatId();
  }

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
    await streamChatResponse(
      currentChatId.value,
      userInput,
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
      requestStatus.value = '请求失败';
      messages.value.push(
        buildUiMessage('system', `请求失败：${error instanceof Error ? error.message : '未知错误'}`),
      );
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
    isSending.value = false;
    abortController.value = null;

    if (!conversations.value.includes(currentChatId.value)) {
      conversations.value.unshift(currentChatId.value);
    }

    try {
      const latest = await fetchConversations();
      if (latest.length) {
        conversations.value = latest;
      }
    } catch (error) {
      console.warn(error);
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
  await sendMessageContent(userInput);
}

async function handleResendMessage(content: string): Promise<void> {
  if (isSending.value) {
    return;
  }
  await sendMessageContent(content);
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
  void bootstrapChatState();
  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
});

watch(draft, () => {
  nextTick(adjustComposerHeight);
});

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
  <main class="chat-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.push('/')">
        <span class="back-button__icon" aria-hidden="true">
          <span class="back-button__chevron"></span>
        </span>
        <span class="back-button__label">返回入口</span>
      </button>
      <div class="page-title">
        <div class="page-title-line">
          <h1>智能聊天</h1>
          <span class="page-title-divider" aria-hidden="true"></span>
          <p>简约对话界面</p>
        </div>
      </div>
    </header>

    <section class="chat-layout">
      <aside class="conversation-sidebar">
        <div class="sidebar-head">
          <h2>会话</h2>
          <button type="button" class="new-chat-button" @click="startNewConversation">新会话</button>
        </div>
        <div v-if="isLoadingConversations" class="sidebar-placeholder">会话加载中...</div>
        <ul v-else-if="conversations.length" class="conversation-list">
          <li v-for="(chatId, index) in conversations" :key="chatId">
            <button
              type="button"
              class="conversation-item"
              :class="{ 'conversation-item--active': chatId === currentChatId }"
              @click="loadHistory(chatId)"
            >
              <span class="conversation-item__title">{{ getConversationTitle(chatId, index) }}</span>
              <span class="conversation-item__subtitle" :title="chatId">{{ getConversationSubtitle(chatId) }}</span>
            </button>
          </li>
        </ul>
        <div v-else class="sidebar-placeholder">还没有会话，先新建一个吧。</div>
      </aside>

      <section class="chat-main">
        <div ref="chatBodyRef" class="message-list" @scroll.passive="handleChatScroll">
          <div v-if="isLoadingHistory" class="placeholder-card">历史消息加载中...</div>
          <div v-else-if="messages.length === 0" class="empty-state">
            <h3>开始聊天</h3>
            <p>在下方输入你的问题，按 Enter 发送。</p>
          </div>
          <ChatMessageBubble
            v-for="message in messages"
            :key="message.id"
            :message-id="message.id"
            :role="message.role"
            :content="message.content"
            :created-at="message.createdAt"
            :streaming="message.streaming"
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
              placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
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
                <button v-if="isSending" type="button" class="ghost-button" @click="stopGenerating">
                  停止
                </button>
                <button type="button" class="primary-button" :disabled="!canSend" @click="handleSendMessage">
                  {{ isSending ? '生成中...' : '发送' }}
                </button>
              </div>
            </div>
          </div>
        </footer>
      </section>
    </section>
  </main>
</template>

<style scoped>
.chat-page {
  width: min(1400px, 100%);
  height: 100dvh;
  margin: 0 auto;
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-sizing: border-box;
  color: #0f172a;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  min-height: 52px;
  padding: 4px 2px;
}

.page-title {
  min-width: 0;
}

.page-title-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 6px 8px 6px 2px;
}

.page-title h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
  letter-spacing: 0.01em;
  white-space: nowrap;
}

.page-title p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}

.page-title-divider {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.9);
  flex-shrink: 0;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94)),
    rgba(255, 255, 255, 0.94);
  color: #1e293b;
  border-radius: 16px;
  padding: 6px 16px 6px 8px;
  cursor: pointer;
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.back-button:hover {
  transform: translateX(-1px);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.1);
}

.back-button:focus-visible {
  outline: none;
  border-color: rgba(59, 130, 246, 0.42);
  box-shadow:
    0 0 0 4px rgba(59, 130, 246, 0.12),
    0 12px 24px rgba(59, 130, 246, 0.08);
}

.back-button__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  flex-shrink: 0;
  border: 1px solid rgba(59, 130, 246, 0.16);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.98), rgba(219, 234, 254, 0.88));
}

.back-button__chevron {
  width: 8px;
  height: 8px;
  border-left: 2px solid #2563eb;
  border-bottom: 2px solid #2563eb;
  transform: translateX(1px) rotate(45deg);
}

.back-button__label {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
  white-space: nowrap;
}

.chat-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
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

  .conversation-sidebar {
    max-height: 220px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .chat-page {
    padding: 10px;
  }

  .page-header {
    gap: 10px;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .page-title-line {
    gap: 8px;
    flex-wrap: wrap;
  }

  .page-title h1 {
    font-size: 20px;
  }

  .page-title p {
    white-space: normal;
  }

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
