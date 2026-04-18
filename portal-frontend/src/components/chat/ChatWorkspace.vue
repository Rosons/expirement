<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import type { ConversationListItem } from '../../types/chat';
import type { ChatWorkspaceApi } from '../../types/chat-workspace';
import {
  ChatComposer,
  ChatMessagePane,
  ConversationSidebar,
  getConversationSubtitle as formatConversationSubtitle,
  getConversationTitle as formatConversationTitle,
  shortenChatId,
  useChatScroll,
  useConversationState,
  useStreamChat,
  type SendLifecyclePayload,
  type SendResultPayload,
} from './workspace';
import type { PendingAttachment } from '../../types/chat';
type ChatComposerExpose = {
  focus: () => void;
  adjustHeight: () => void;
};

const props = withDefaults(
  defineProps<{
    chatApi: ChatWorkspaceApi;
    showConversationSidebar?: boolean;
    showNewConversationButton?: boolean;
    showMessageCopyAction?: boolean;
    showMessageResendAction?: boolean;
    showTrailingPanel?: boolean;
    initialMessage?: string;
    emptyTitle?: string;
    emptyDescription?: string;
    composerPlaceholder?: string;
    /** 仅加载该会话：不展示左侧列表时与路由会话 id 对齐 */
    fixedSessionChatId?: string;
    /** 历史为空时插入的本地客服欢迎语（不请求后端） */
    localWelcomeAssistantMessage?: string;
    /** 是否启用附件上传功能 */
    enableAttachment?: boolean;
  }>(),
  {
    showConversationSidebar: true,
    showNewConversationButton: true,
    showMessageCopyAction: true,
    showMessageResendAction: true,
    showTrailingPanel: false,
    initialMessage: '',
    emptyTitle: '开始聊天',
    emptyDescription: '在下方输入你的问题，按 Enter 发送。',
    composerPlaceholder: '输入你的问题，Enter 发送，Shift+Enter 换行',
    fixedSessionChatId: '',
    localWelcomeAssistantMessage: '',
    enableAttachment: false,
  },
);
const emit = defineEmits<{
  beforeSend: [payload: SendLifecyclePayload];
  afterSend: [payload: SendResultPayload];
  conversationChange: [chatId: string];
}>();

const draft = ref('');
const attachmentMode = ref(false);
const chatBodyRef = ref<HTMLElement | null>(null);
const composerRef = ref<ChatComposerExpose | null>(null);
const consumedInitialMessage = ref(false);
let scrollToBottomImpl: (options?: { immediate?: boolean; retryCount?: number; force?: boolean }) => void = () => undefined;
let resetUnreadStateImpl: () => void = () => undefined;

const {
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
} = useConversationState({
  chatApi: props.chatApi,
  chatBodyRef,
  resetUnreadState: () => resetUnreadStateImpl(),
  scrollToBottom: (options) => scrollToBottomImpl(options),
  fixedSessionChatId: props.fixedSessionChatId,
  localWelcomeAssistantMessage: props.localWelcomeAssistantMessage,
});

const {
  shouldAutoScroll,
  hasUnseenNewMessages,
  handleChatScroll,
  scrollToBottom,
  scrollToLatestMessage,
  markNewMessageIfNeeded,
  resetUnreadState,
} = useChatScroll({
  chatBodyRef,
  hasMoreOlder,
  isLoadingOlder,
  isLoadingHistory,
  loadOlderMessages: () => loadOlderMessages(),
});
scrollToBottomImpl = scrollToBottom;
resetUnreadStateImpl = resetUnreadState;

const { isSending, requestStatus, sendMessageContent, stopGenerating } = useStreamChat({
  chatApi: props.chatApi,
  currentChatId,
  messages,
  conversations,
  draft,
  resetUnreadState,
  scrollToBottom,
  adjustComposerHeight,
  focusComposer,
  onBeforeSend: (payload) => emit('beforeSend', payload),
  onAfterSend: (payload) => emit('afterSend', payload),
});

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
  return shortenChatId(chatId);
});

function focusComposer(): void {
  nextTick(() => {
    composerRef.value?.focus();
  });
}

function adjustComposerHeight(): void {
  composerRef.value?.adjustHeight();
}

function handleDraftUpdate(value: string): void {
  draft.value = value;
}

function handleBodyElementChange(element: HTMLElement | null): void {
  chatBodyRef.value = element;
}

function getConversationTitle(item: ConversationListItem, index: number): string {
  return formatConversationTitle(item, index, currentChatId.value);
}

function getConversationSubtitle(item: ConversationListItem): string {
  return formatConversationSubtitle(item);
}

function startNewConversation(): void {
  startNewConversationBase();
  draft.value = '';
  requestStatus.value = '';
  resetUnreadState();
  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
}

async function handleComposerSend(files: PendingAttachment[]): Promise<void> {
  const userInput = draft.value.trim();
  if (isSending.value) {
    return;
  }
  draft.value = '';
  nextTick(adjustComposerHeight);
  await sendMessageContent(userInput, 'user', files);
}

async function handleResendMessage(content: string): Promise<void> {
  if (isSending.value) {
    return;
  }
  await sendMessageContent(content, 'resend');
}

onMounted(() => {
  void (async () => {
    await bootstrapChatState();
    const entryMessage = props.initialMessage.trim();
    const hasFixedSession = !!props.fixedSessionChatId?.trim();
    const hasHistoryMessages = messages.value.length > 0;
    // 固定会话且已有历史消息时，不发送 initialMessage（避免从 URL 分享的会话重复发送 gameStart）
    if (!consumedInitialMessage.value && entryMessage && !(hasFixedSession && hasHistoryMessages)) {
      consumedInitialMessage.value = true;
      await sendMessageContent(entryMessage, 'initial');
    }
  })();

  nextTick(() => {
    adjustComposerHeight();
    focusComposer();
  });
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
  () => attachmentMode.value,
  (val) => {
    if (val) {
      nextTick(() => scrollToBottom({ immediate: true, retryCount: 2, force: true }));
    }
  },
);

watch(
  () => {
    const last = messages.value.at(-1);
    return `${messages.value.length}:${last?.id ?? ''}:${last?.content.length ?? 0}`;
  },
  () => {
    if (markNewMessageIfNeeded()) {
      return;
    }
    scrollToBottom({ immediate: true, retryCount: 2 });
  },
);

</script>

<template>
  <section
    class="chat-layout"
    :class="{
      'chat-layout--single': !showConversationSidebar,
      'chat-layout--with-trailing': showTrailingPanel,
    }"
  >
    <ConversationSidebar
      v-if="showConversationSidebar"
      :is-loading-conversations="isLoadingConversations"
      :show-new-conversation-button="showNewConversationButton"
      :conversations="conversations"
      :current-chat-id="currentChatId"
      :title-resolver="getConversationTitle"
      :subtitle-resolver="getConversationSubtitle"
      @new-conversation="startNewConversation"
      @select-conversation="loadHistory"
    />

    <section class="chat-main">
      <ChatMessagePane
        :messages="messages"
        :is-loading-history="isLoadingHistory"
        :is-loading-older="isLoadingOlder"
        :has-more-older="hasMoreOlder"
        :empty-title="emptyTitle"
        :empty-description="emptyDescription"
        :show-message-copy-action="showMessageCopyAction"
        :show-message-resend-action="showMessageResendAction"
        :should-auto-scroll="shouldAutoScroll"
        :has-unseen-new-messages="hasUnseenNewMessages"
        :on-body-element-change="handleBodyElementChange"
        @scroll="handleChatScroll"
        @resend-message="handleResendMessage"
        @scroll-to-latest="scrollToLatestMessage"
      />

      <ChatComposer
        ref="composerRef"
        v-if="enableAttachment"
        v-model:attachment-mode="attachmentMode"
        :draft="draft"
        :is-sending="isSending"
        :can-send="canSend"
        :composer-placeholder="composerPlaceholder"
        :short-chat-id="shortChatId"
        :total-message-count="totalMessageCount"
        :draft-length="draftLength"
        :request-status="requestStatus"
        :show-attachment-toggle="enableAttachment"
        @update:draft="handleDraftUpdate"
        @send="handleComposerSend"
        @stop="stopGenerating"
      />
      <ChatComposer
        v-else
        ref="composerRef"
        :draft="draft"
        :is-sending="isSending"
        :can-send="canSend"
        :composer-placeholder="composerPlaceholder"
        :short-chat-id="shortChatId"
        :total-message-count="totalMessageCount"
        :draft-length="draftLength"
        :request-status="requestStatus"
        :show-attachment-toggle="false"
        @update:draft="handleDraftUpdate"
        @send="handleComposerSend"
        @stop="stopGenerating"
      />
    </section>

    <aside v-if="showTrailingPanel" class="chat-trailing-panel">
      <slot name="trailing" />
    </aside>
  </section>
</template>

<style scoped>
.chat-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  border-radius: 22px;
  overflow: visible;
}

.chat-layout--single {
  grid-template-columns: minmax(0, 1fr);
}

.chat-layout--with-trailing {
  grid-template-columns: 280px minmax(0, 1fr) minmax(300px, 380px);
}

.chat-trailing-panel {
  min-height: 0;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(16px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-main {
  min-height: 0;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(16px);
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

@media (max-width: 1024px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-layout--single {
    grid-template-columns: 1fr;
  }

  .chat-layout--with-trailing {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(0, 1fr) minmax(200px, 38vh);
  }

}

</style>
