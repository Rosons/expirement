<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

const props = defineProps<{
  draft: string;
  isSending: boolean;
  canSend: boolean;
  composerPlaceholder: string;
  shortChatId: string;
  totalMessageCount: number;
  draftLength: number;
  requestStatus: string;
}>();

const emit = defineEmits<{
  'update:draft': [value: string];
  send: [];
  stop: [];
}>();

const inputRef = ref<HTMLTextAreaElement | null>(null);

function focus(): void {
  inputRef.value?.focus();
}

function adjustHeight(): void {
  const input = inputRef.value;
  if (!input) {
    return;
  }

  input.style.height = 'auto';
  const nextHeight = Math.min(Math.max(input.scrollHeight, 64), 220);
  input.style.height = `${nextHeight}px`;
  input.style.overflowY = input.scrollHeight > 220 ? 'auto' : 'hidden';
}

function handleInput(event: Event): void {
  const target = event.target;
  if (!(target instanceof HTMLTextAreaElement)) {
    return;
  }
  emit('update:draft', target.value);
  nextTick(adjustHeight);
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    if (props.canSend) {
      emit('send');
    }
  }
}

watch(
  () => props.draft,
  () => {
    nextTick(adjustHeight);
  },
);

defineExpose({
  focus,
  adjustHeight,
});
</script>

<template>
  <footer class="composer">
    <div class="composer-shell">
      <textarea
        ref="inputRef"
        :value="draft"
        class="composer-input"
        :placeholder="composerPlaceholder"
        :disabled="isSending"
        @input="handleInput"
        @keydown="handleKeydown"
      />
      <div class="composer-actions">
        <div class="composer-meta">
          <span>当前会话：{{ shortChatId }}</span>
          <span>{{ totalMessageCount }} 条消息</span>
          <span v-if="draftLength > 0">输入中 {{ draftLength }} 字</span>
          <span v-if="requestStatus">{{ requestStatus }}</span>
        </div>
        <div class="action-buttons">
          <el-button v-if="isSending" class="ghost-button" @click="emit('stop')">停止</el-button>
          <el-button type="primary" class="primary-button" :loading="isSending" :disabled="!canSend" @click="emit('send')">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.composer {
  flex-shrink: 0;
  padding: 10px 0 0;
  background: transparent;
}

.composer-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.84);
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.composer-input {
  width: 100%;
  min-height: 64px;
  max-height: 220px;
  resize: none;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 16px;
  padding: 16px 18px;
  font-size: 15px;
  line-height: 1.75;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.98);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
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
  min-height: 40px;
  border-radius: 11px;
  padding: 10px 16px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  color: #fff;
  background: linear-gradient(140deg, #2563eb, #1d4ed8);
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.22);
}

.primary-button:disabled,
.primary-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-button {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.98);
  border-color: rgba(148, 163, 184, 0.4);
}

.ghost-button:not(.is-disabled):hover {
  border-color: rgba(59, 130, 246, 0.45);
  background: rgba(239, 246, 255, 0.95);
}

.primary-button:not(.is-disabled):hover {
  filter: brightness(1.03);
}

.ghost-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .composer {
    padding: 10px 0 0;
  }
}
</style>
