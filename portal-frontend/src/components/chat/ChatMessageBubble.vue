<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ChatRole } from '../../types/chat';
import { renderSafeMarkdown } from '../../utils/markdown';

const props = defineProps<{
  messageId: string;
  role: ChatRole;
  content: string;
  createdAt: number;
  streaming?: boolean;
}>();
const emit = defineEmits<{
  resendMessage: [content: string];
}>();

const renderedHtml = computed(() => renderSafeMarkdown(props.content));
const canCopyMessage = computed(() => props.content.trim().length > 0);
const canResendMessage = computed(() => props.role === 'user' && !props.streaming && props.content.trim().length > 0);
const messageCopyLabel = ref('复制');
const roleLabel = computed(() => {
  if (props.role === 'user') {
    return '用户';
  }
  if (props.role === 'assistant') {
    return 'AI 助手';
  }
  return '系统提示';
});
const avatarLabel = computed(() => {
  if (props.role === 'user') {
    return '你';
  }
  if (props.role === 'assistant') {
    return 'AI';
  }
  return '系';
});
const formattedTime = computed(() => {
  if (!Number.isFinite(props.createdAt)) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(props.createdAt);
});

async function copyCodeText(text: string): Promise<void> {
  if (!text) {
    return;
  }

  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.left = '-9999px';
  document.body.appendChild(textArea);
  textArea.select();
  document.execCommand('copy');
  document.body.removeChild(textArea);
}

function showCopyStatus(button: HTMLButtonElement, label: string): void {
  const previousLabel = button.textContent || '复制';
  button.textContent = label;
  window.setTimeout(() => {
    button.textContent = previousLabel;
  }, 1400);
}

async function handleMarkdownClick(event: MouseEvent): Promise<void> {
  const clickedElement = event.target as HTMLElement | null;
  const copyButton = clickedElement?.closest('.md-code-block__copy') as HTMLButtonElement | null;
  if (!copyButton) {
    return;
  }

  const codeElement = copyButton
    .closest('.md-code-block')
    ?.querySelector('pre code') as HTMLElement | null;
  const codeText = codeElement?.innerText ?? '';
  if (!codeText.trim()) {
    showCopyStatus(copyButton, '无内容');
    return;
  }

  try {
    await copyCodeText(codeText);
    showCopyStatus(copyButton, '已复制');
  } catch (error) {
    console.error(error);
    showCopyStatus(copyButton, '复制失败');
  }
}

function showMessageCopyStatus(label: string): void {
  messageCopyLabel.value = label;
  window.setTimeout(() => {
    messageCopyLabel.value = '复制';
  }, 1400);
}

function blurActionButton(event: MouseEvent): void {
  const button = event.currentTarget as HTMLButtonElement | null;
  button?.blur();
}

async function handleCopyMessage(event: MouseEvent): Promise<void> {
  if (!canCopyMessage.value) {
    showMessageCopyStatus('无内容');
    blurActionButton(event);
    return;
  }
  try {
    await copyCodeText(props.content);
    showMessageCopyStatus('已复制');
  } catch (error) {
    console.error(error);
    showMessageCopyStatus('复制失败');
  }
  blurActionButton(event);
}

function handleResendMessage(event: MouseEvent): void {
  if (!canResendMessage.value) {
    blurActionButton(event);
    return;
  }
  emit('resendMessage', props.content);
  blurActionButton(event);
}
</script>

<template>
  <article class="message-row" :class="`message-row--${props.role}`">
    <div class="avatar-shell">
      <div class="avatar">
        {{ avatarLabel }}
      </div>
    </div>
    <div class="bubble-wrap">
      <header class="bubble-meta" :class="{ 'bubble-meta--streaming': props.streaming }">
        <div class="bubble-meta__main">
          <span class="bubble-role">{{ roleLabel }}</span>
          <span v-if="formattedTime" class="bubble-time">{{ formattedTime }}</span>
        </div>
        <span v-if="props.streaming" class="streaming-pill">实时生成中</span>
      </header>
      <div class="bubble">
        <div
          class="markdown-body"
          :key="`${props.messageId}-${props.content.length}`"
          v-html="renderedHtml"
          @click="handleMarkdownClick"
        />
        <span v-if="props.streaming" class="cursor" />
      </div>
      <div v-if="canCopyMessage || canResendMessage" class="bubble-toolbar">
        <button
          v-if="canCopyMessage"
          type="button"
          class="meta-action-button"
          @click="handleCopyMessage"
        >
          {{ messageCopyLabel }}
        </button>
        <button
          v-if="canResendMessage"
          type="button"
          class="meta-action-button"
          @click="handleResendMessage"
        >
          重发
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.message-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  max-width: 100%;
}

.message-row--user {
  flex-direction: row-reverse;
}

.message-row--system {
  margin: 0 auto;
}

.avatar-shell {
  padding: 2px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
  color: #fff;
}

.bubble-wrap {
  width: fit-content;
  max-width: min(100%, 880px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-row--user .bubble-wrap {
  align-items: flex-end;
}

.message-row--system .bubble-wrap {
  max-width: min(100%, 760px);
}

.bubble-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 6px;
}

.bubble-meta--streaming {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.message-row--user .bubble-meta {
  flex-direction: row-reverse;
}

.bubble-meta__main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bubble-role {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.bubble-time {
  font-size: 12px;
  color: #94a3b8;
}

.streaming-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.82);
  border: 1px solid rgba(59, 130, 246, 0.22);
  flex-shrink: 0;
}

.bubble-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px;
  flex-wrap: wrap;
  opacity: 0;
  transform: translateY(-4px);
  pointer-events: none;
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.meta-action-button {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.82);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.meta-action-button:hover {
  border-color: rgba(59, 130, 246, 0.28);
  background: rgba(239, 246, 255, 0.88);
  color: #1d4ed8;
}

.message-row:hover .bubble-toolbar,
.message-row:focus-within .bubble-toolbar {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.message-row--user .avatar {
  background: linear-gradient(160deg, #2563eb, #1d4ed8);
}

.message-row--assistant .avatar {
  background: linear-gradient(160deg, #0f766e, #0ea5e9);
}

.message-row--system .avatar {
  background: linear-gradient(160deg, #475569, #334155);
}

.bubble {
  --bubble-text: #1e293b;
  --bubble-accent: #2563eb;
  width: 100%;
  max-width: 100%;
  padding: 16px 18px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.84);
  box-shadow:
    0 18px 38px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  position: relative;
}

.message-row--user .bubble {
  --bubble-text: #0f172a;
  --bubble-accent: #1d4ed8;
  background:
    linear-gradient(145deg, rgba(219, 234, 254, 0.96), rgba(191, 219, 254, 0.82)),
    rgba(219, 234, 254, 0.88);
  border-color: rgba(96, 165, 250, 0.28);
}

.message-row--assistant .bubble {
  --bubble-accent: #0f766e;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.88)),
    rgba(255, 255, 255, 0.9);
}

.message-row--system .bubble {
  --bubble-text: #334155;
  --bubble-accent: #475569;
  background: rgba(248, 250, 252, 0.84);
  border-style: dashed;
}

.cursor {
  display: inline-flex;
  width: 8px;
  height: 16px;
  margin-left: 3px;
  margin-top: 4px;
  background: var(--bubble-accent);
  border-radius: 999px;
  animation: blink 1.05s steps(1, end) infinite;
}

.markdown-body {
  font-size: 14px;
  color: var(--bubble-text);
  line-height: 1.82;
  overflow-wrap: anywhere;
  max-width: 100%;
}

:deep(.markdown-body > *:first-child) {
  margin-top: 0;
}

:deep(.markdown-body > *:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-body p) {
  margin: 0 0 12px;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  margin: 0 0 12px;
  padding-left: 22px;
}

:deep(.markdown-body blockquote) {
  margin: 0 0 12px;
  padding: 10px 12px;
  color: #475569;
  border-left: 3px solid rgba(59, 130, 246, 0.5);
  background: rgba(59, 130, 246, 0.08);
  border-radius: 8px;
}

:deep(.markdown-body code) {
  font-family:
    'JetBrains Mono',
    'Consolas',
    monospace;
  font-size: 13px;
  background: rgba(15, 23, 42, 0.06);
  padding: 2px 5px;
  border-radius: 6px;
}

:deep(.markdown-body pre) {
  margin: 0 0 12px;
  padding: 12px;
  border-radius: 14px;
  overflow: auto;
  background: #0b1220;
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: inset 0 0 0 1px rgba(30, 41, 59, 0.22);
}

:deep(.markdown-body pre code) {
  background: transparent;
  padding: 0;
  color: #e2e8f0;
}

:deep(.markdown-body .md-code-block) {
  margin: 0 0 12px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

:deep(.markdown-body .md-code-block__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  background: #111827;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
}

:deep(.markdown-body .md-code-block__lang) {
  font-size: 12px;
  color: #cbd5e1;
  text-transform: lowercase;
}

:deep(.markdown-body .md-code-block__copy) {
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(15, 23, 42, 0.7);
  color: #e2e8f0;
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

:deep(.markdown-body .md-code-block__copy:hover) {
  background: rgba(30, 41, 59, 0.9);
}

:deep(.markdown-body .md-code-block pre) {
  margin: 0;
  border: none;
  box-shadow: none;
}

:deep(.markdown-body a) {
  color: #2563eb;
  text-decoration: underline;
}

@media (max-width: 720px) {
  .message-row {
    gap: 10px;
  }

  .avatar {
    width: 36px;
    height: 36px;
  }

  .bubble {
    padding: 14px;
    border-radius: 20px;
  }

  .bubble-meta {
    padding: 0 2px;
  }
}

@keyframes blink {
  0%,
  40% {
    opacity: 1;
  }
  41%,
  100% {
    opacity: 0;
  }
}
</style>
