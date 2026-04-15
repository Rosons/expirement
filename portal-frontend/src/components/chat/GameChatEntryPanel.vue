<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
  disabled?: boolean;
}>();

const emit = defineEmits<{
  start: [reason: string];
}>();

const reasonDraft = ref('');
const reasonLength = computed(() => reasonDraft.value.trim().length);

function handleStart(): void {
  if (props.disabled) {
    return;
  }
  emit('start', reasonDraft.value);
}
</script>

<template>
  <section class="game-entry">
    <div class="game-entry__card">
      <header class="game-entry__head">
        <p class="game-entry__eyebrow">恋爱哄哄模拟器</p>
        <h2>先告诉我，她为什么生气</h2>
        <p>可选填写。你点“开始游戏”后，系统会把这段话当作第一条消息发送；留空则默认发送“开始游戏”。</p>
      </header>

      <textarea
        v-model="reasonDraft"
        class="game-entry__textarea"
        :disabled="disabled"
        placeholder="例如：她觉得我最近回复太慢，还忘记了约会纪念日..."
      />

      <div class="game-entry__footer">
        <span v-if="reasonLength > 0">已输入 {{ reasonLength }} 字</span>
        <span v-else>不填也可以直接开始</span>
        <button type="button" class="game-entry__button" :disabled="disabled" @click="handleStart">
          开始游戏
        </button>
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
}

.game-entry__card {
  width: min(840px, 100%);
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  box-shadow: 0 20px 42px rgba(15, 23, 42, 0.08);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.game-entry__head {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.game-entry__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #2563eb;
  text-transform: uppercase;
  font-weight: 700;
}

.game-entry__head h2 {
  margin: 0;
  font-size: clamp(24px, 3.3vw, 34px);
  color: #0f172a;
  line-height: 1.22;
}

.game-entry__head p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.game-entry__textarea {
  width: 100%;
  min-height: 240px;
  max-height: 360px;
  resize: vertical;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 16px 18px;
  font-size: 15px;
  line-height: 1.75;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.95);
}

.game-entry__textarea:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.68);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.game-entry__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.game-entry__button {
  border: 1px solid transparent;
  border-radius: 12px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #2563eb, #1d4ed8);
  cursor: pointer;
}

.game-entry__button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .game-entry__card {
    padding: 18px;
    border-radius: 20px;
  }

  .game-entry__textarea {
    min-height: 200px;
  }

  .game-entry__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
