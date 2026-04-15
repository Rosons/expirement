<script setup lang="ts">
import { useRouter } from 'vue-router';
import GameChatEntryPanel from '../components/chat/GameChatEntryPanel.vue';

const router = useRouter();

async function handleStartGame(reason: string): Promise<void> {
  const entryMessage = reason.trim() || '开始游戏';
  await router.push({
    name: 'product-game-chat-play',
    query: {
      gameStart: entryMessage,
    },
  });
}
</script>

<template>
  <main class="game-entry-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.push('/')">
        <span class="back-button__icon" aria-hidden="true">
          <span class="back-button__chevron"></span>
        </span>
        <span class="back-button__label">返回入口</span>
      </button>
      <div class="page-title">
        <div class="page-title-line">
          <h1>哄哄模拟器</h1>
          <span class="page-title-divider" aria-hidden="true"></span>
          <p>先设定场景，再进入对话</p>
        </div>
      </div>
    </header>

    <GameChatEntryPanel @start="handleStartGame" />
  </main>
</template>

<style scoped>
.game-entry-page {
  width: min(1400px, 100%);
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
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94)),
    rgba(255, 255, 255, 0.94);
  color: #1e293b;
  border-radius: 16px;
  padding: 6px 16px 6px 8px;
  cursor: pointer;
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.back-button:hover {
  transform: translateX(-1px);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.1);
}

.back-button:focus-visible {
  outline: none;
  border-color: rgba(59, 130, 246, 0.42);
  box-shadow:
    0 0 0 4px rgba(59, 130, 246, 0.12),
    0 12px 24px rgba(59, 130, 246, 0.08);
}

.back-button__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  flex-shrink: 0;
  border: 1px solid rgba(59, 130, 246, 0.16);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.98), rgba(219, 234, 254, 0.88));
}

.back-button__chevron {
  width: 8px;
  height: 8px;
  border-left: 2px solid #2563eb;
  border-bottom: 2px solid #2563eb;
  transform: translateX(1px) rotate(45deg);
}

.back-button__label {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .game-entry-page {
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
