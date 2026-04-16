<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';

const props = defineProps<{
  disabled?: boolean;
}>();

const emit = defineEmits<{
  start: [reason: string];
}>();

const SUGGESTED_REASON = '她觉得我最近回复太慢，还忘记了约会纪念日。';
const reasonDraft = ref('');
const reasonLength = computed(() => reasonDraft.value.trim().length);
const reasonInputRef = ref<{ focus: () => void } | null>(null);

function handleStart(): void {
  if (props.disabled) {
    return;
  }
  emit('start', reasonDraft.value);
}

function focusReasonInput(): void {
  requestAnimationFrame(() => {
    reasonInputRef.value?.focus();
  });
}

function handleInputKeydown(event: Event | KeyboardEvent): void {
  if (!(event instanceof KeyboardEvent)) {
    return;
  }
  if (event.isComposing) {
    return;
  }
  if (event.key === 'Tab' && reasonDraft.value.trim().length === 0) {
    event.preventDefault();
    reasonDraft.value = SUGGESTED_REASON;
    nextTick(() => focusReasonInput());
    return;
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    handleStart();
  }
}

onMounted(async () => {
  await nextTick();
  focusReasonInput();
});
</script>

<template>
  <section class="game-entry">
    <div class="game-entry__card">
      <header class="game-entry__head">
        <p class="game-entry__eyebrow">恋爱哄哄模拟器</p>
        <h2>她为什么生气</h2>
        <p class="game-entry__hint">一句话描述她为什么生气，留空则默认发送“开始游戏”。</p>
      </header>

      <el-input
        ref="reasonInputRef"
        v-model="reasonDraft"
        class="game-entry__textarea"
        type="textarea"
        :disabled="disabled"
        :autosize="{ minRows: 8, maxRows: 12 }"
        resize="vertical"
        :placeholder="`例如：${SUGGESTED_REASON}`"
        @keydown="handleInputKeydown"
      />

      <div class="game-entry__footer">
        <div class="game-entry__meta">
          <span class="game-entry__counter">已输入 {{ reasonLength }} 字</span>
          <span class="game-entry__shortcut">Tab 填充示例 · Enter 发送 · Shift + Enter 换行</span>
        </div>
        <el-button type="primary" class="game-entry__button" :disabled="disabled" @click="handleStart">
          开始游戏
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.game-entry {
  flex: 1;
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 4px;
}

.game-entry__card {
  width: min(860px, 100%);
  border-radius: 24px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  background:
    radial-gradient(circle at 92% 4%, rgba(59, 130, 246, 0.15), transparent 38%),
    radial-gradient(circle at 8% 100%, rgba(14, 165, 233, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.95));
  box-shadow:
    0 22px 46px rgba(15, 23, 42, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.88);
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.game-entry__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.game-entry__eyebrow {
  width: fit-content;
  margin: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #1d4ed8;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.game-entry__head h2 {
  margin: 0;
  font-size: clamp(25px, 3.1vw, 34px);
  color: #0b1220;
  line-height: 1.16;
}

.game-entry__hint {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #475569;
}

.game-entry__textarea {
  width: 100%;
}

.game-entry__textarea :deep(.el-textarea__inner) {
  min-height: 240px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  padding: 16px 18px;
  font-size: 15px;
  line-height: 1.76;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.92);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.game-entry__textarea :deep(.el-textarea__inner:focus) {
  border-color: rgba(59, 130, 246, 0.62);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.game-entry__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.game-entry__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.game-entry__counter {
  color: #64748b;
  font-size: 13px;
}

.game-entry__shortcut {
  color: #94a3b8;
  font-size: 12px;
}

.game-entry__button {
  min-height: 42px;
  border-radius: 12px;
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.22);
}

.game-entry__button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 720px) {
  .game-entry__card {
    padding: 18px;
    border-radius: 18px;
  }

  .game-entry__textarea :deep(.el-textarea__inner) {
    min-height: 200px;
  }

  .game-entry__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .game-entry__button {
    width: 100%;
  }
}
</style>
