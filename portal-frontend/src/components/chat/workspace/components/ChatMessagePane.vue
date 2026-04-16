<script setup lang="ts">
import { Bottom } from '@element-plus/icons-vue';
import type { ComponentPublicInstance } from 'vue';
import ChatMessageBubble from '../../ChatMessageBubble.vue';
import type { UiChatMessage } from '../../../../types/chat';

const props = defineProps<{
  messages: UiChatMessage[];
  isLoadingHistory: boolean;
  isLoadingOlder: boolean;
  hasMoreOlder: boolean;
  emptyTitle: string;
  emptyDescription: string;
  showMessageCopyAction: boolean;
  showMessageResendAction: boolean;
  shouldAutoScroll: boolean;
  hasUnseenNewMessages: boolean;
  onBodyElementChange?: (element: HTMLElement | null) => void;
}>();

const emit = defineEmits<{
  scroll: [];
  resendMessage: [content: string];
  scrollToLatest: [];
}>();

function setBodyElement(element: Element | ComponentPublicInstance | null): void {
  props.onBodyElementChange?.(element instanceof HTMLElement ? element : null);
}
</script>

<template>
  <div :ref="setBodyElement" class="message-list" @scroll.passive="emit('scroll')">
    <div v-if="isLoadingOlder" class="history-load-hint" aria-live="polite">加载更早消息中…</div>
    <div v-else-if="hasMoreOlder && messages.length > 0" class="history-load-hint history-load-hint--muted">上滑加载更早消息</div>
    <div v-if="isLoadingHistory" class="placeholder-skeleton">
      <el-skeleton animated :rows="4" />
    </div>
    <el-empty v-else-if="messages.length === 0" class="empty-state" :image-size="92">
      <template #description>
        <h3 class="empty-state__title">{{ emptyTitle }}</h3>
        <p class="empty-state__desc">{{ emptyDescription }}</p>
      </template>
    </el-empty>
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
      @resend-message="emit('resendMessage', $event)"
    />
  </div>

  <el-button
    v-if="!shouldAutoScroll && hasUnseenNewMessages"
    class="scroll-bottom-fab"
    size="small"
    :icon="Bottom"
    round
    @click="emit('scrollToLatest')"
  >
    回到底部
  </el-button>
</template>

<style scoped>
.message-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 22px 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  border-radius: 16px 16px 12px 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92)),
    rgba(255, 255, 255, 0.92);
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

.empty-state,
.placeholder-skeleton {
  margin: auto;
  width: min(100%, 560px);
  padding: 22px;
  text-align: center;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92)),
    rgba(255, 255, 255, 0.92);
  color: #64748b;
}

.placeholder-skeleton :deep(.el-skeleton__item) {
  height: 18px;
  border-radius: 10px;
}

.placeholder-skeleton :deep(.el-skeleton__paragraph .el-skeleton__item:first-child) {
  width: 70%;
  height: 24px;
}

.empty-state {
  padding: 18px 22px;
}

.empty-state__title {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.empty-state__desc {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #64748b;
}

.empty-state :deep(.el-empty__description) {
  margin-top: 0;
}

.scroll-bottom-fab {
  position: absolute;
  right: 20px;
  bottom: clamp(160px, 20vh, 210px);
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

@media (max-width: 720px) {
  .message-list {
    padding: 18px 14px 12px;
  }

  .scroll-bottom-fab {
    right: 12px;
    bottom: clamp(146px, 22vh, 190px);
  }
}
</style>
