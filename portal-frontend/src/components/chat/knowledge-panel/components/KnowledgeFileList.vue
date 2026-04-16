<script setup lang="ts">
import { Delete, Download, View } from '@element-plus/icons-vue';
import type { ChatFileListItem } from '../../../../types/chat-file';
import { fileExtension, formatBytes, formatShortTime } from '../helpers/file-utils';

defineProps<{
  hasConversation: boolean;
  isListLoading: boolean;
  files: ChatFileListItem[];
  deletingFileId: string | null;
}>();

const emit = defineEmits<{
  preview: [file: ChatFileListItem];
  download: [file: ChatFileListItem];
  delete: [file: ChatFileListItem];
}>();

function extensionToneClass(filename: string): string {
  const ext = (fileExtension(filename) || '').toLowerCase();

  if (['pdf'].includes(ext)) return 'kb__ext--pdf';
  if (['doc', 'docx', 'rtf', 'odt'].includes(ext)) return 'kb__ext--doc';
  if (['xls', 'xlsx', 'csv'].includes(ext)) return 'kb__ext--sheet';
  if (['ppt', 'pptx', 'key'].includes(ext)) return 'kb__ext--slide';
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp'].includes(ext)) return 'kb__ext--image';
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return 'kb__ext--archive';
  if (['mp3', 'wav', 'flac', 'm4a', 'aac'].includes(ext)) return 'kb__ext--audio';
  if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) return 'kb__ext--video';
  if (['ts', 'tsx', 'js', 'jsx', 'py', 'java', 'go', 'cpp', 'c', 'php', 'json', 'xml', 'yaml', 'yml', 'md'].includes(ext)) {
    return 'kb__ext--code';
  }

  return 'kb__ext--default';
}
</script>

<template>
  <section class="kb__list-section">
    <header class="kb__list-header">
      <h3>文件列表</h3>
      <span>{{ files.length }} 项</span>
    </header>
    <div class="kb__list-body">
      <el-empty v-if="!hasConversation" class="kb__empty" description="暂无会话" :image-size="84" />
      <div v-else-if="isListLoading && !files.length" class="kb__skeleton">
        <el-skeleton animated :rows="4" />
      </div>
      <el-empty v-else-if="!files.length" class="kb__empty" description="暂无文件，上传后即可在对话中使用" :image-size="84" />
      <ul v-else class="kb__list" role="list">
        <li v-for="item in files" :key="item.fileId" class="kb__row" :class="{ 'kb__row--busy': deletingFileId === item.fileId }">
          <div class="kb__row-main">
            <div class="kb__name-line">
              <span class="kb__name" :title="item.originalFilename">{{ item.originalFilename }}</span>
              <span class="kb__ext" :class="extensionToneClass(item.originalFilename)">{{ fileExtension(item.originalFilename) || 'FILE' }}</span>
            </div>
            <div class="kb__meta">
              <span class="kb__meta-text">{{ formatBytes(item.fileSize) }}</span>
              <span v-if="formatShortTime(item.createdAt)" class="kb__meta-text">
                {{ formatShortTime(item.createdAt) }}
              </span>
            </div>
            <div class="kb__actions">
              <el-button class="kb__chip kb__chip--icon" :icon="View" title="预览" aria-label="预览" @click="emit('preview', item)" />
              <el-button
                class="kb__chip kb__chip--icon"
                :icon="Download"
                title="下载"
                aria-label="下载"
                @click="emit('download', item)"
              />
              <el-button
                class="kb__chip kb__chip--danger kb__chip--icon"
                :icon="Delete"
                :loading="deletingFileId === item.fileId"
                title="删除"
                aria-label="删除"
                :disabled="deletingFileId === item.fileId"
                @click="emit('delete', item)"
              />
            </div>
          </div>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.kb__list-section {
  flex: 1;
  min-height: 0;
  margin-top: 2px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.kb__list-header {
  flex-shrink: 0;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.kb__list-header h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.kb__list-header span {
  font-size: 12px;
  color: #64748b;
}

.kb__list-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 10px 10px 12px;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.5) transparent;
  overscroll-behavior: contain;
}

.kb__empty {
  padding: 20px 12px;
  text-align: center;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.kb__empty :deep(.el-empty__description p) {
  margin: 0;
  font-size: 13px;
  color: #7a8ba3;
}

.kb__skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 4px;
}

.kb__skeleton :deep(.el-skeleton__item) {
  height: 16px;
  border-radius: 10px;
}

.kb__skeleton :deep(.el-skeleton__paragraph .el-skeleton__item:first-child) {
  width: 66%;
  height: 22px;
}

.kb__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb__row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  padding: 10px 11px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.kb__row:hover {
  border-color: #bfdbfe;
  background: #f3f7ff;
}

.kb__row--busy {
  opacity: 0.55;
  pointer-events: none;
}

.kb__row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.kb__name-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.kb__name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  flex: 1;
}

.kb__ext {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 1.1;
  color: #475569;
  border: 1px solid #dbe3ec;
  border-radius: 999px;
  padding: 3px 7px;
  background: #ffffff;
}

.kb__ext--default {
  color: #475569;
  border-color: #dbe3ec;
  background: #ffffff;
}

.kb__ext--pdf {
  color: #9f1239;
  border-color: #fecdd3;
  background: #fff1f2;
}

.kb__ext--doc {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.kb__ext--sheet {
  color: #166534;
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.kb__ext--slide {
  color: #b45309;
  border-color: #fde68a;
  background: #fffbeb;
}

.kb__ext--image {
  color: #7c3aed;
  border-color: #ddd6fe;
  background: #f5f3ff;
}

.kb__ext--archive {
  color: #6b21a8;
  border-color: #e9d5ff;
  background: #faf5ff;
}

.kb__ext--audio {
  color: #0f766e;
  border-color: #99f6e4;
  background: #f0fdfa;
}

.kb__ext--video {
  color: #be123c;
  border-color: #fecdd3;
  background: #fff1f2;
}

.kb__ext--code {
  color: #4338ca;
  border-color: #c7d2fe;
  background: #eef2ff;
}

.kb__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.kb__meta-text {
  position: relative;
  font-size: 12px;
  line-height: 1.2;
  color: #64748b;
}

.kb__meta-text + .kb__meta-text {
  padding-left: 10px;
}

.kb__meta-text + .kb__meta-text::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #cbd5e1;
  transform: translateY(-50%);
}

.kb__actions {
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: flex-start;
}

.kb__chip {
  border: none;
  background: transparent;
  border-radius: 0;
  min-height: 0;
  padding: 0;
  font-size: 0;
  font-weight: 400;
  color: #475569;
  box-shadow: none;
  cursor: pointer;
  transition:
    color 0.12s ease,
    transform 0.12s ease,
    opacity 0.12s ease;
}

.kb__chip:hover:not(:disabled) {
  background: transparent;
  color: #1d4ed8;
  transform: translateY(-1px);
}

.kb__chip--icon {
  width: 20px;
  min-width: 20px;
  height: 20px;
  min-height: 20px;
  padding: 0;
  border-radius: 0;
}

.kb__chip--danger {
  color: #b91c1c;
}

.kb__chip--danger:hover:not(:disabled) {
  background: transparent;
  color: #991b1b;
}

.kb__chip:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.kb__chip :deep(.el-icon) {
  font-size: 14px;
}

@media (max-width: 960px) {
  .kb__actions {
    padding-top: 1px;
  }
}
</style>
