<script setup lang="ts">
import { Refresh, Upload } from '@element-plus/icons-vue';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { CHAT_PRODUCT_TYPE_KNOWLEDGE } from '../../constants/chat-product-types';
import { confirmDanger } from '../../feedback';
import { deleteChatFile, fetchChatFilesByConversation, uploadChatFile } from '../../services/chat';
import type { ChatFileListItem } from '../../types/chat-file';
import { KnowledgeFileList, KnowledgePreviewDialog, useFilePreview } from './knowledge-panel';

const props = defineProps<{
  conversationId: string;
}>();

const files = ref<ChatFileListItem[]>([]);
const isListLoading = ref(false);
const isUploading = ref(false);
const deletingFileId = ref<string | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);

const hasConversation = computed(() => props.conversationId.trim().length > 0);
const canUploadFiles = computed(() => hasConversation.value);
const {
  previewOpen,
  previewTitle,
  previewKind,
  previewText,
  previewImageUrl,
  previewPdfUrl,
  revokePreviewUrl,
  closePreview,
  handleDownload,
  handlePreview,
} = useFilePreview();

async function loadFiles(): Promise<void> {
  if (!hasConversation.value) {
    files.value = [];
    return;
  }
  isListLoading.value = true;
  try {
    files.value = await fetchChatFilesByConversation(props.conversationId);
  } finally {
    isListLoading.value = false;
  }
}

function onFileInputChange(event: Event): void {
  const target = event.target;
  if (!(target instanceof HTMLInputElement)) {
    return;
  }
  void handleUploadFiles(target.files);
}

async function handleUploadFiles(fileList: FileList | File[] | null): Promise<void> {
  if (!canUploadFiles.value || !fileList?.length || isUploading.value) {
    return;
  }
  const list = Array.from(fileList);
  isUploading.value = true;
  try {
    for (const file of list) {
      await uploadChatFile({
        conversationId: props.conversationId,
        file,
        conversationType: CHAT_PRODUCT_TYPE_KNOWLEDGE,
      });
    }
    await loadFiles();
  } finally {
    isUploading.value = false;
    if (fileInputRef.value) {
      fileInputRef.value.value = '';
    }
  }
}

async function handleDelete(file: ChatFileListItem): Promise<void> {
  if (deletingFileId.value || !hasConversation.value) {
    return;
  }
  const confirmed = await confirmDanger(`确定删除「${file.originalFilename}」？`, {
    title: '删除文件',
    confirmText: '删除',
  });
  if (!confirmed) {
    return;
  }
  deletingFileId.value = file.fileId;
  try {
    await deleteChatFile(file.fileId, props.conversationId);
    await loadFiles();
  } finally {
    deletingFileId.value = null;
  }
}

watch(
  () => props.conversationId,
  () => {
    closePreview();
    void loadFiles();
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  closePreview();
});
</script>

<template>
  <div class="kb">
    <header class="kb__header">
      <h2 class="kb__title">知识库</h2>
      <el-button
        class="kb__refresh"
        size="small"
        plain
        :icon="Refresh"
        :disabled="!hasConversation"
        :loading="isListLoading"
        @click="void loadFiles()"
      >
        刷新
      </el-button>
    </header>

    <div
      class="kb__drop"
      :class="{ 'kb__drop--disabled': !canUploadFiles, 'kb__drop--drag': isDragging && canUploadFiles }"
      @dragenter.prevent="isDragging = true"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="
        isDragging = false;
        void handleUploadFiles($event.dataTransfer?.files ?? null);
      "
    >
      <input
        ref="fileInputRef"
        type="file"
        class="kb__input"
        multiple
        :disabled="!canUploadFiles || isUploading"
        @change="onFileInputChange"
      />
      <!-- <div v-if="!hasConversation" class="kb__drop-inner kb__drop-inner--muted">
        <span class="kb__drop-emoji" aria-hidden="true">📂</span>
        <p>请先在左侧选择或创建会话</p>
      </div> -->
      <div class="kb__drop-inner">
        <p class="kb__drop-line">拖拽文件到此处，或点击下方按钮</p>
        <p class="kb__drop-note">单次可上传多个文件，上传后可直接预览与下载</p>
        <el-button type="default" class="kb__btn-primary" :icon="Upload" :loading="isUploading" @click="fileInputRef?.click()">
          {{ isUploading ? '上传中…' : '选择文件' }}
        </el-button>
      </div>
    </div>

    <KnowledgeFileList
      :has-conversation="hasConversation"
      :is-list-loading="isListLoading"
      :files="files"
      :deleting-file-id="deletingFileId"
      @preview="void handlePreview($event)"
      @download="void handleDownload($event)"
      @delete="void handleDelete($event)"
    />

    <KnowledgePreviewDialog
      v-model="previewOpen"
      :title="previewTitle"
      :kind="previewKind"
      :text="previewText"
      :image-url="previewImageUrl"
      :pdf-url="previewPdfUrl"
      @closed="revokePreviewUrl"
    />
  </div>
</template>

<style scoped>
.kb {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px;
  box-sizing: border-box;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
}

.kb__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 0;
}

.kb__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #0f172a;
  line-height: 1.3;
}

.kb__refresh {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 7px 13px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease,
    background 0.15s ease;
}

.kb__refresh:hover:not(:disabled) {
  border-color: #bfdbfe;
  color: #1e40af;
  background: #eff6ff;
}

.kb__refresh:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.kb__refresh:focus-visible,
.kb__btn-primary:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.kb__drop {
  flex-shrink: 0;
  border-radius: 14px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  padding: 18px 14px;
  text-align: center;
  transition:
    border-color 0.18s ease,
    background 0.18s ease;
}

.kb__drop--drag:not(.kb__drop--disabled) {
  border-color: #93c5fd;
  background: #eff6ff;
}

.kb__drop--disabled {
  opacity: 0.64;
  border-color: #e2e8f0;
  background: #f8fafc;
}

.kb__input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.kb__drop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.kb__drop-inner--muted p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.kb__drop-emoji {
  font-size: 26px;
  line-height: 1;
  opacity: 0.9;
}

.kb__drop-line {
  margin: 0;
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.kb__drop-note {
  margin: -2px 0 2px;
  font-size: 12px;
  color: #64748b;
}

.kb__btn-primary {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  min-height: 36px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  transition:
    color 0.14s ease,
    border-color 0.14s ease,
    background-color 0.14s ease,
    box-shadow 0.14s ease;
}

.kb__btn-primary:hover:not(:disabled) {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.12);
}

.kb__btn-primary:disabled {
  opacity: 0.66;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 1200px) {
  .kb {
    border-radius: 16px;
    padding: 10px;
  }
}

@media (max-width: 960px) {
  .kb {
    gap: 12px;
    padding: 9px;
  }
}

@media (max-width: 640px) {
  .kb__drop {
    padding: 18px 12px;
  }
}
</style>
