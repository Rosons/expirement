<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import ChatWorkspace from '../../components/chat/ChatWorkspace.vue';
import { gameChatWorkspaceApi } from '../../services/chat';
import { createChatId } from '../../components/chat/workspace';

const route = useRoute();
const router = useRouter();

const sessionId = ref('');

const SESSION_QUERY_KEYS = ['sessionId', 'chatId'] as const;

function readSessionIdFromRoute(): string {
  for (const key of SESSION_QUERY_KEYS) {
    const raw = route.query[key];
    if (typeof raw === 'string' && raw.trim()) {
      return raw.trim();
    }
  }
  return '';
}

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

onMounted(async () => {
  let id = readSessionIdFromRoute();
  if (!id) {
    id = createChatId();
    await router.replace({ path: route.path, query: { ...route.query, sessionId: id } });
  }
  sessionId.value = id;
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
      v-if="sessionId"
      :chat-api="gameChatWorkspaceApi"
      :initial-message="initialMessage"
      :fixed-session-chat-id="sessionId"
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
