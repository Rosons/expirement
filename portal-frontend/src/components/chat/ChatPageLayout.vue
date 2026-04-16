<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRouter } from 'vue-router';

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

const router = useRouter();
const layoutStyle = computed(() => ({
  '--chat-page-max-width': props.maxWidth,
}));
</script>

<template>
  <main class="chat-page" :style="layoutStyle">
    <header class="page-header">
      <el-button class="back-button" :icon="ArrowLeft" @click="router.push(props.backTo)">
        {{ props.backText }}
      </el-button>
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

.back-button {
  min-height: 40px;
  border: 1px solid #dbe4ee;
  background: #ffffff;
  color: #334155;
  border-radius: 999px;
  padding: 6px 14px 6px 10px;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.back-button :deep(.el-icon) {
  margin-right: 2px;
  font-size: 13px;
}

.back-button:hover {
  border-color: #bfdbfe;
  background-color: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.14);
}

.back-button:focus-visible {
  outline: none;
  border-color: #93c5fd;
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.16),
    0 8px 18px rgba(59, 130, 246, 0.12);
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
