<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import ChatWorkspace from '../../components/chat/ChatWorkspace.vue';
import { createChatId } from '../../components/chat/workspace/helpers/chat-workspace-helpers';
import { customerChatWorkspaceApi } from '../../services/chat';

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

const CUSTOMER_WELCOME =
  '您好呀，我是课程咨询助手小甜甜，很高兴为您服务。为了给您推荐更合适的课程，请先告诉我您的兴趣方向和学员学历，我再为您安排课程咨询和试听预约哦。';

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
  <ChatPageLayout title="智能客服" subtitle="会话式服务" back-to="/">
    <ChatWorkspace
      v-if="sessionId"
      :chat-api="customerChatWorkspaceApi"
      :show-conversation-sidebar="false"
      :show-new-conversation-button="false"
      :fixed-session-chat-id="sessionId"
      :local-welcome-assistant-message="CUSTOMER_WELCOME"
      empty-title="开始课程咨询"
      empty-description="请先告诉我您的兴趣方向和学员学历，我会为您推荐合适课程并协助试听预约。"
      composer-placeholder="描述您的问题，Enter 发送，Shift+Enter 换行"
    />
  </ChatPageLayout>
</template>
