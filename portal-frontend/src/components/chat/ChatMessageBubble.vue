<script setup lang="ts">
import { CopyDocument, RefreshRight } from '@element-plus/icons-vue';
import type { ChatRole } from '../../types/chat';
import { useMessageBubble } from './message-bubble';

const props = withDefaults(
  defineProps<{
    messageId: string;
    role: ChatRole;
    content: string;
    createdAt: number;
    streaming?: boolean;
    enableCopyAction?: boolean;
    enableResendAction?: boolean;
  }>(),
  {
    enableCopyAction: true,
    enableResendAction: true,
  },
);
const emit = defineEmits<{
  resendMessage: [content: string];
}>();

const {
  renderedHtml,
  canCopyMessage,
  canResendMessage,
  roleLabel,
  avatarLabel,
  formattedTime,
  showAssistantStreamingPlaceholder,
  handleMarkdownClick,
  handleCopyMessage,
  handleResendMessage,
} = useMessageBubble(props, (content) => {
  emit('resendMessage', content);
});
</script>

<template>
  <article class="message-row" :class="`message-row--${props.role}`">
    <div class="avatar-shell">
      <div class="avatar">
        {{ avatarLabel }}
      </div>
    </div>
    <div class="bubble-wrap">
      <header class="bubble-meta">
        <div class="bubble-meta__main">
          <span class="bubble-role">{{ roleLabel }}</span>
          <span v-if="formattedTime" class="bubble-time">{{ formattedTime }}</span>
        </div>
      </header>
      <div class="bubble">
        <span v-if="showAssistantStreamingPlaceholder" class="streaming-hint">正在回复中…</span>
        <div
          v-if="!showAssistantStreamingPlaceholder"
          class="markdown-body"
          :key="`${props.messageId}-${props.content.length}`"
          v-html="renderedHtml"
          @click="handleMarkdownClick"
        />
        <span v-if="props.streaming && !showAssistantStreamingPlaceholder" class="cursor" />
      </div>
      <div v-if="canCopyMessage || canResendMessage" class="bubble-toolbar">
        <el-tooltip v-if="canCopyMessage" content="复制" placement="top" :show-after="150">
          <el-button
            class="meta-action-button"
            text
            :icon="CopyDocument"
            aria-label="复制"
            @click="handleCopyMessage"
          />
        </el-tooltip>
        <el-tooltip v-if="canResendMessage" content="重发" placement="top" :show-after="150">
          <el-button
            class="meta-action-button"
            text
            :icon="RefreshRight"
            aria-label="重发"
            @click="handleResendMessage"
          />
        </el-tooltip>
      </div>
    </div>
  </article>
</template>

<style scoped src="./message-bubble/styles/chat-message-bubble.css"></style>
