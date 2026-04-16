export { default as ChatComposer } from './components/ChatComposer.vue';
export { default as ChatMessagePane } from './components/ChatMessagePane.vue';
export { default as ConversationSidebar } from './components/ConversationSidebar.vue';

export { useChatScroll } from './composables/use-chat-scroll';
export { useConversationState } from './composables/use-conversation-state';
export { useStreamChat } from './composables/use-stream-chat';
export type { SendLifecyclePayload, SendResultPayload } from './composables/use-stream-chat';

export {
  appendAssistantChunk,
  buildUiMessage,
  createChatId,
  finalizeAssistantMessage,
  getConversationSubtitle,
  getConversationTitle,
  shortenChatId,
} from './helpers/chat-workspace-helpers';
