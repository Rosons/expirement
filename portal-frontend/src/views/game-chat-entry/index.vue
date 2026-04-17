<script setup lang="ts">
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import GameChatEntryPanel from '../../components/chat/GameChatEntryPanel.vue';
import { useGameChatEntryView } from './composables/use-game-chat-entry-view';

const { handleStartGame } = useGameChatEntryView();
</script>

<template>
  <ChatPageLayout
    title="哄哄模拟器"
    subtitle="先设定场景，再进入对话"
    back-to="/"
    back-text="返回入口"
  >
    <GameChatEntryPanel @start="handleStartGame" />
  </ChatPageLayout>
</template>
