<script setup lang="ts">
import { useRouter } from 'vue-router';

const props = withDefaults(
  defineProps<{
    to: string;
    text?: string;
  }>(),
  {
    text: '返回',
  },
);

const router = useRouter();
</script>

<template>
  <button class="back-btn" @click="router.push(props.to)">
    <svg class="back-btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
      <path d="M19 12H5M12 19l-7-7 7-7" />
    </svg>
    <span class="back-btn__text">{{ props.text }}</span>
  </button>
</template>

<style scoped>
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 40px;
  padding: 8px 16px 8px 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
  color: #334155;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  box-shadow:
    0 4px 12px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: all 0.2s ease;
  backdrop-filter: blur(8px);
}

.back-btn:hover {
  transform: translateX(-2px);
  border-color: rgba(59, 130, 246, 0.35);
  background: linear-gradient(135deg, rgba(255, 255, 255, 1), rgba(239, 246, 255, 0.95));
  color: #1d4ed8;
  box-shadow:
    0 6px 20px rgba(59, 130, 246, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.back-btn:active {
  transform: translateX(0);
  box-shadow:
    0 2px 8px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.back-btn__icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.back-btn:hover .back-btn__icon {
  transform: translateX(-2px);
}

.back-btn__text {
  white-space: nowrap;
}
</style>
