<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue';
import type { ConversationListItem } from '../../../../types/chat';

defineProps<{
  isLoadingConversations: boolean;
  showNewConversationButton: boolean;
  conversations: ConversationListItem[];
  currentChatId: string;
  titleResolver: (item: ConversationListItem, index: number) => string;
  subtitleResolver: (item: ConversationListItem) => string;
}>();

const emit = defineEmits<{
  newConversation: [];
  selectConversation: [chatId: string];
}>();
</script>

<template>
  <aside class="conversation-sidebar">
    <div class="sidebar-head">
      <h2>会话</h2>
      <el-button
        v-if="showNewConversationButton"
        type="default"
        size="small"
        class="new-chat-button"
        :icon="Plus"
        @click="emit('newConversation')"
      >
        新会话
      </el-button>
    </div>
    <div v-if="isLoadingConversations" class="sidebar-skeleton">
      <el-skeleton :rows="3" animated />
    </div>
    <ul v-else-if="conversations.length" class="conversation-list">
      <li v-for="(item, index) in conversations" :key="item.id">
        <button
          type="button"
          class="conversation-item"
          :class="{ 'conversation-item--active': item.id === currentChatId }"
          @click="emit('selectConversation', item.id)"
        >
          <span class="conversation-item__title">{{ titleResolver(item, index) }}</span>
          <span class="conversation-item__subtitle" :title="item.id">{{ subtitleResolver(item) }}</span>
        </button>
      </li>
    </ul>
    <el-empty v-else class="sidebar-empty" description="还没有会话，先新建一个吧。" :image-size="82" />
  </aside>
</template>

<style scoped>
.conversation-sidebar {
  min-height: 0;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.sidebar-head h2 {
  margin: 0;
  font-size: 16px;
  line-height: 1.2;
  letter-spacing: 0.01em;
  font-weight: 700;
  color: #0f172a;
}

.new-chat-button {
  min-height: 32px;
  border: 1px solid #dbe4ee;
  border-radius: 999px;
  padding: 7px 12px;
  background: #f8fafc;
  color: #1e293b;
  font-weight: 600;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.new-chat-button :deep(.el-icon) {
  margin-right: 2px;
  font-size: 13px;
}

.new-chat-button:hover {
  border-color: #bfdbfe;
  background-color: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08);
}

.new-chat-button:focus-visible {
  outline: none;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.14);
}

.conversation-list {
  list-style: none;
  padding: 2px 4px 2px 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.conversation-list::-webkit-scrollbar {
  width: 8px;
}

.conversation-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.4);
}

.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-skeleton {
  padding: 8px 4px;
}

.sidebar-skeleton :deep(.el-skeleton__item) {
  height: 44px;
  border-radius: 12px;
}

.sidebar-empty {
  margin: auto 0;
  padding: 8px 0 14px;
}

.sidebar-empty :deep(.el-empty__description p) {
  font-size: 13px;
  color: #64748b;
}

.conversation-item {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
  padding: 12px 12px 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.conversation-item:hover {
  border-color: #e2e8f0;
  background: #f1f5f9;
  transform: translateX(1px);
}

.conversation-item--active {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #f4f8ff 0%, #eef5ff 100%);
}

.conversation-item--active .conversation-item__title {
  color: #1e3a8a;
}

.conversation-item__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
}

.conversation-item__subtitle {
  max-width: 100%;
  font-size: 12px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

@media (max-width: 1024px) {
  .conversation-sidebar {
    max-height: 220px;
  }
}
</style>
