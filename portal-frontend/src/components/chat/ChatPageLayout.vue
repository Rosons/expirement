<script setup lang="ts">
import { computed } from 'vue';
import BackButton from './BackButton.vue';

const props = withDefaults(
  defineProps<{
    title: string;
    subtitle: string;
    backTo: string;
    backText?: string;
    maxWidth?: string;
  }>(),
  {
    backText: '返回入口',
    maxWidth: '1400px',
  },
);

const layoutStyle = computed(() => ({
  '--chat-page-max-width': props.maxWidth,
}));
</script>

<template>
  <main class="chat-page" :style="layoutStyle">
    <header class="page-header">
      <BackButton :to="props.backTo" :text="props.backText" />
      <div class="page-title">
        <div class="page-title-line">
          <h1>{{ props.title }}</h1>
          <span class="page-title-divider" aria-hidden="true"></span>
          <p>{{ props.subtitle }}</p>
        </div>
      </div>
    </header>

    <slot />
  </main>
</template>

<style scoped>
.chat-page {
  width: min(var(--chat-page-max-width), 100%);
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
}
</style>
