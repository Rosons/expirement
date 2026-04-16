<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';
import GameChatEntryPanel from '../../components/chat/GameChatEntryPanel.vue';
import { useGameChatEntryView } from './composables/use-game-chat-entry-view';

const { handleStartGame, goHome } = useGameChatEntryView();
</script>

<template>
  <main class="game-entry-page">
    <header class="page-header">
      <el-button class="back-button" :icon="ArrowLeft" @click="goHome">返回入口</el-button>
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
<style scoped src="./styles/game-chat-entry-view.css"></style>
