<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import ChatWorkspace from '../../components/chat/ChatWorkspace.vue';
import { gameChatWorkspaceApi } from '../../services/chat';

const route = useRoute();

const initialMessage = computed(() => {
  const value = route.query.gameStart;
  if (Array.isArray(value)) {
    return value[0]?.trim() ?? '';
  }
  if (typeof value === 'string') {
    return value.trim();
  }
  return '';
});
</script>

<template>
  <ChatPageLayout
    title="哄哄模拟器"
    subtitle="情绪安抚对话中"
    back-to="/products/game-chat"
    back-text="返回设定"
  >
    <ChatWorkspace
      :chat-api="gameChatWorkspaceApi"
      :initial-message="initialMessage"
      :show-conversation-sidebar="false"
      :show-new-conversation-button="false"
      :show-message-copy-action="false"
      :show-message-resend-action="false"
      empty-title="开始哄哄模式"
      empty-description="你可以继续发送消息，尝试更温柔、共情和具体的表达。"
      composer-placeholder="继续哄她...（Enter 发送，Shift + Enter 换行）"
    />
  </ChatPageLayout>
</template>
