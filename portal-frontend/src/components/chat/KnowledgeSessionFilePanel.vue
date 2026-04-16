<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { CHAT_PRODUCT_TYPE_KNOWLEDGE } from '../../constants/chat-product-types';
import {
  deleteChatFile,
  downloadChatFileBlob,
  fetchChatFilesByConversation,
  uploadChatFile,
} from '../../services/chat/chat-file-service';
import type { ChatFileListItem } from '../../types/chat-file';

const props = defineProps<{
  conversationId: string;
}>();

const files = ref<ChatFileListItem[]>([]);
const isListLoading = ref(false);
const isUploading = ref(false);
const deletingFileId = ref<string | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);

const previewOpen = ref(false);
const previewTitle = ref('');
const previewKind = ref<'image' | 'pdf' | 'text' | 'unsupported'>('unsupported');
const previewText = ref('');
const previewImageUrl = ref('');
const previewPdfUrl = ref('');
let previewObjectUrl: string | null = null;

const hasConversation = computed(() => props.conversationId.trim().length > 0);
const canUploadFiles = computed(() => hasConversation.value);

function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let n = bytes;
  let u = 0;
  while (n >= 1024 && u < units.length - 1) {
    n /= 1024;
    u += 1;
  }
  return `${n < 10 && u > 0 ? n.toFixed(1) : Math.round(n)} ${units[u]}`;
}

function formatShortTime(iso: string | null | undefined): string {
  if (!iso?.trim()) {
    return '';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function fileExtension(name: string): string {
  const i = name.lastIndexOf('.');
  if (i < 0 || i === name.length - 1) {
    return '';
  }
  return name.slice(i + 1).toUpperCase().slice(0, 4);
}

function guessMimeFromFilename(name: string): string | undefined {
  const lower = name.toLowerCase();
  if (lower.endsWith('.pdf')) {
    return 'application/pdf';
  }
  if (lower.endsWith('.png')) {
    return 'image/png';
  }
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (lower.endsWith('.gif')) {
    return 'image/gif';
  }
  if (lower.endsWith('.webp')) {
    return 'image/webp';
  }
  if (lower.endsWith('.txt') || lower.endsWith('.md') || lower.endsWith('.log')) {
    return 'text/plain';
  }
  if (lower.endsWith('.json')) {
    return 'application/json';
  }
  if (lower.endsWith('.csv')) {
    return 'text/csv';
  }
  return undefined;
}

function effectiveContentType(file: ChatFileListItem): string {
  const fromMeta = file.contentType?.trim();
  if (fromMeta) {
    return fromMeta;
  }
  return guessMimeFromFilename(file.originalFilename) ?? 'application/octet-stream';
}

function revokePreviewUrl(): void {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl);
    previewObjectUrl = null;
  }
  previewImageUrl.value = '';
  previewPdfUrl.value = '';
  previewText.value = '';
}

function closePreview(): void {
  previewOpen.value = false;
  revokePreviewUrl();
}

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

async function handleDownload(file: ChatFileListItem): Promise<void> {
  const blob = await downloadChatFileBlob(file.fileId);
  const mime = effectiveContentType(file);
  const typed = blob.type && blob.type !== 'application/octet-stream' ? blob : new Blob([blob], { type: mime });
  const url = URL.createObjectURL(typed);
  const a = document.createElement('a');
  a.href = url;
  a.download = file.originalFilename || 'download';
  a.rel = 'noopener';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2_000);
}

async function handlePreview(file: ChatFileListItem): Promise<void> {
  closePreview();
  previewTitle.value = file.originalFilename;
  const blob = await downloadChatFileBlob(file.fileId);
  const mime = effectiveContentType(file);
  const typed = blob.type && blob.type !== 'application/octet-stream' ? blob : new Blob([blob], { type: mime });

  if (mime.startsWith('image/')) {
    previewKind.value = 'image';
    previewObjectUrl = URL.createObjectURL(typed);
    previewImageUrl.value = previewObjectUrl;
    previewOpen.value = true;
    return;
  }

  if (mime === 'application/pdf' || file.originalFilename.toLowerCase().endsWith('.pdf')) {
    const pdfBlob = new Blob([await typed.arrayBuffer()], { type: 'application/pdf' });
    previewKind.value = 'pdf';
    previewObjectUrl = URL.createObjectURL(pdfBlob);
    previewPdfUrl.value = previewObjectUrl;
    previewOpen.value = true;
    return;
  }

  if (mime.startsWith('text/') || mime === 'application/json' || mime === 'application/csv') {
    previewKind.value = 'text';
    previewText.value = await typed.text();
    previewOpen.value = true;
    return;
  }

  previewKind.value = 'unsupported';
  previewOpen.value = true;
}

async function handleDelete(file: ChatFileListItem): Promise<void> {
  if (deletingFileId.value || !hasConversation.value) {
    return;
  }
  const ok = window.confirm(`确定删除「${file.originalFilename}」？`);
  if (!ok) {
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
      <div class="kb__header-text">
        <h2 class="kb__title">会话资料</h2>
        <p class="kb__subtitle">
          {{ hasConversation ? `共 ${files.length} 个文件` : '选择左侧会话后管理附件' }}
        </p>
      </div>
      <button
        type="button"
        class="kb__refresh"
        :disabled="!hasConversation || isListLoading"
        title="刷新列表"
        @click="void loadFiles()"
      >
        <span class="kb__refresh-icon" aria-hidden="true">↻</span>
        刷新
      </button>
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
      <div v-if="!hasConversation" class="kb__drop-inner kb__drop-inner--muted">
        <span class="kb__drop-emoji" aria-hidden="true">📂</span>
        <p>请先在左侧选择或创建会话</p>
      </div>
      <div v-else class="kb__drop-inner">
        <span class="kb__drop-emoji" aria-hidden="true">⬆</span>
        <p class="kb__drop-line">拖拽文件到此处，或点击下方按钮</p>
        <button type="button" class="kb__btn-primary" :disabled="isUploading" @click="fileInputRef?.click()">
          {{ isUploading ? '上传中…' : '选择文件' }}
        </button>
      </div>
    </div>

    <section class="kb__list-section">
      <div v-if="!hasConversation" class="kb__empty">暂无会话</div>
      <div v-else-if="isListLoading && !files.length" class="kb__skeleton">
        <div v-for="n in 3" :key="n" class="kb__sk-row" />
      </div>
      <div v-else-if="!files.length" class="kb__empty">暂无文件，上传后即可在对话中使用</div>
      <ul v-else class="kb__list" role="list">
        <li
          v-for="f in files"
          :key="f.fileId"
          class="kb__row"
          :class="{ 'kb__row--busy': deletingFileId === f.fileId }"
        >
          <div class="kb__badge" :title="f.originalFilename">{{ fileExtension(f.originalFilename) || 'FILE' }}</div>
          <div class="kb__row-main">
            <span class="kb__name" :title="f.originalFilename">{{ f.originalFilename }}</span>
            <span class="kb__meta">
              {{ formatBytes(f.fileSize) }}
              <span v-if="formatShortTime(f.createdAt)" class="kb__dot">·</span>
              {{ formatShortTime(f.createdAt) }}
            </span>
          </div>
          <div class="kb__actions">
            <button type="button" class="kb__chip" @click="void handlePreview(f)">预览</button>
            <button type="button" class="kb__chip" @click="void handleDownload(f)">下载</button>
            <button
              type="button"
              class="kb__chip kb__chip--danger"
              :disabled="deletingFileId === f.fileId"
              @click="void handleDelete(f)"
            >
              {{ deletingFileId === f.fileId ? '删除中…' : '删除' }}
            </button>
          </div>
        </li>
      </ul>
    </section>

    <Teleport to="body">
      <div v-if="previewOpen" class="kb-preview-bg" role="dialog" aria-modal="true" @click.self="closePreview">
        <div class="kb-preview">
          <header class="kb-preview__head">
            <h3 class="kb-preview__title">{{ previewTitle }}</h3>
            <button type="button" class="kb-preview__close" @click="closePreview">关闭</button>
          </header>
          <div class="kb-preview__body">
            <img v-if="previewKind === 'image'" class="kb-preview__img" :src="previewImageUrl" alt="" />
            <iframe v-else-if="previewKind === 'pdf'" class="kb-preview__frame" title="PDF" :src="previewPdfUrl" />
            <pre v-else-if="previewKind === 'text'" class="kb-preview__pre">{{ previewText }}</pre>
            <p v-else class="kb-preview__muted">此格式请使用「下载」查看。</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.kb {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 2px 8px;
  box-sizing: border-box;
}

.kb__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 4px 0;
}

.kb__title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #0f172a;
  line-height: 1.25;
}

.kb__subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

.kb__refresh {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: linear-gradient(180deg, #fff, #f8fafc);
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease;
}

.kb__refresh:hover:not(:disabled) {
  border-color: rgba(59, 130, 246, 0.45);
  color: #1d4ed8;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
}

.kb__refresh:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.kb__refresh-icon {
  display: inline-block;
  font-size: 14px;
  line-height: 1;
}

.kb__drop {
  flex-shrink: 0;
  border-radius: 18px;
  border: 1px dashed rgba(99, 102, 241, 0.38);
  background: linear-gradient(145deg, rgba(238, 242, 255, 0.9), rgba(255, 255, 255, 0.65));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
  padding: 18px 16px;
  text-align: center;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.kb__drop--drag:not(.kb__drop--disabled) {
  border-color: rgba(79, 70, 229, 0.65);
  background: linear-gradient(145deg, rgba(224, 231, 255, 0.95), rgba(255, 255, 255, 0.8));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 0 0 3px rgba(99, 102, 241, 0.12);
}

.kb__drop--disabled {
  opacity: 0.68;
  border-color: rgba(203, 213, 225, 0.9);
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
  gap: 10px;
}

.kb__drop-inner--muted p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.kb__drop-emoji {
  font-size: 28px;
  line-height: 1;
  filter: saturate(1.1);
}

.kb__drop-line {
  margin: 0;
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.kb__btn-primary {
  border: none;
  border-radius: 12px;
  padding: 9px 20px;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  box-shadow: 0 10px 22px rgba(79, 70, 229, 0.28);
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease,
    filter 0.12s ease;
}

.kb__btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.03);
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.32);
}

.kb__btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.kb__list-section {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 2px 4px 6px;
}

.kb__empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(248, 250, 252, 0.65);
}

.kb__skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px 0;
}

.kb__sk-row {
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 40%, #f1f5f9 80%);
  background-size: 200% 100%;
  animation: kb-shimmer 1.1s ease-in-out infinite;
}

@keyframes kb-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

.kb__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kb__row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.kb__row:hover {
  border-color: rgba(199, 210, 254, 0.95);
  box-shadow: 0 10px 26px rgba(79, 70, 229, 0.07);
}

.kb__row--busy {
  opacity: 0.55;
  pointer-events: none;
}

.kb__badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #4338ca;
  background: linear-gradient(145deg, #eef2ff, #e0e7ff);
  border: 1px solid rgba(165, 180, 252, 0.55);
  flex-shrink: 0;
}

.kb__row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb__name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb__meta {
  font-size: 11px;
  color: #64748b;
}

.kb__dot {
  margin: 0 2px;
  opacity: 0.65;
}

.kb__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.kb__chip {
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: #f8fafc;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
  transition:
    background 0.12s ease,
    border-color 0.12s ease,
    color 0.12s ease;
}

.kb__chip:hover:not(:disabled) {
  background: #fff;
  border-color: rgba(199, 210, 254, 0.95);
  color: #1d4ed8;
}

.kb__chip--danger {
  color: #b91c1c;
  border-color: rgba(254, 202, 202, 0.85);
  background: rgba(254, 242, 242, 0.65);
}

.kb__chip--danger:hover:not(:disabled) {
  color: #991b1b;
  border-color: rgba(252, 165, 165, 0.95);
}

.kb__chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.kb-preview-bg {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(4px);
}

.kb-preview {
  width: min(920px, 100%);
  max-height: min(88vh, 880px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.22);
  overflow: hidden;
}

.kb-preview__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.95);
  background: linear-gradient(180deg, #fafafa, #fff);
}

.kb-preview__title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
}

.kb-preview__close {
  flex-shrink: 0;
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  padding: 7px 14px;
  font-weight: 700;
  font-size: 13px;
  background: #fff;
  cursor: pointer;
}

.kb-preview__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px 18px 18px;
}

.kb-preview__img {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  border-radius: 12px;
}

.kb-preview__frame {
  width: 100%;
  min-height: 68vh;
  border: none;
  border-radius: 14px;
  background: #f1f5f9;
}

.kb-preview__pre {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  color: #334155;
}

.kb-preview__muted {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}
</style>
