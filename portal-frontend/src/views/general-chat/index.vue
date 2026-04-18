<script setup lang="ts">
import ChatPageLayout from '../../components/chat/ChatPageLayout.vue';
import ChatWorkspace from '../../components/chat/ChatWorkspace.vue';
import { generalChatWorkspaceApi } from '../../services/chat';
</script>

<template>
  <ChatPageLayout title="智能聊天" subtitle="简约对话界面" back-to="/">
    <ChatWorkspace :chat-api="generalChatWorkspaceApi" enable-attachment />
  </ChatPageLayout>
</template>
