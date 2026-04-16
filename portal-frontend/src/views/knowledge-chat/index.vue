<script setup lang="ts">
import { ref } from 'vue';
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import ChatWorkspace from '../../components/chat/ChatWorkspace.vue';
import KnowledgeSessionFilePanel from '../../components/chat/KnowledgeSessionFilePanel.vue';
import { knowledgeChatWorkspaceApi } from '../../services/chat';

const activeConversationId = ref('');
</script>

<template>
  <ChatPageLayout title="知识问答" subtitle="会话聊天 + 本地上传资料" back-to="/" max-width="1680px">
    <ChatWorkspace
      show-trailing-panel
      :chat-api="knowledgeChatWorkspaceApi"
      empty-title="基于会话的知识问答"
      empty-description="左侧管理会话，中间与模型对话，右侧上传文件作为本会话资料。"
      composer-placeholder="结合右侧资料提问，Enter 发送，Shift+Enter 换行"
      @conversation-change="activeConversationId = $event"
    >
      <template #trailing>
        <KnowledgeSessionFilePanel :conversation-id="activeConversationId" />
      </template>
    </ChatWorkspace>
  </ChatPageLayout>
</template>
