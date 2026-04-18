<script setup lang="ts">
import { Close, Paperclip } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, nextTick, ref, watch } from 'vue';
import type { PendingAttachment } from '../../../../types/chat';

const props = defineProps<{
  draft: string;
  isSending: boolean;
  canSend: boolean;
  composerPlaceholder: string;
  shortChatId: string;
  totalMessageCount: number;
  draftLength: number;
  requestStatus: string;
  /** 是否显示附件模式切换按钮 */
  showAttachmentToggle?: boolean;
  /** 当前是否为附件模式（true=附件模式，点击attach后进入，发送后自动退出） */
  attachmentMode?: boolean;
}>();

const emit = defineEmits<{
  'update:draft': [value: string];
  'update:attachmentMode': [value: boolean];
  send: [files: PendingAttachment[]];
  stop: [];
}>();

const inputRef = ref<HTMLTextAreaElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

/** 附件列表 */
const pendingFiles = ref<PendingAttachment[]>([]);

const hasPendingFiles = computed(() => pendingFiles.value.length > 0);

const effectiveCanSend = computed(() => {
  if (props.isSending) return false;
  if (props.attachmentMode) {
    return props.draft.trim().length > 0;
  }
  return props.canSend;
});

function focus(): void {
  inputRef.value?.focus();
}

function adjustHeight(): void {
  const input = inputRef.value;
  if (!input) {
    return;
  }

  input.style.height = 'auto';
  const nextHeight = Math.min(Math.max(input.scrollHeight, 64), 220);
  input.style.height = `${nextHeight}px`;
  input.style.overflowY = input.scrollHeight > 220 ? 'auto' : 'hidden';
}

function handleInput(event: Event): void {
  const target = event.target;
  if (!(target instanceof HTMLTextAreaElement)) {
    return;
  }
  emit('update:draft', target.value);
  nextTick(adjustHeight);
}

function handleKeydown(event: Event | KeyboardEvent): void {
  if (!(event instanceof KeyboardEvent)) {
    return;
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    if (effectiveCanSend.value) {
      handleSend();
    }
  }
}

watch(
  () => props.draft,
  () => {
    nextTick(adjustHeight);
  },
);

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** 允许的文本文档扩展名（小写，不含点） */
const ALLOWED_DOCUMENT_EXTENSIONS = new Set([
  'pdf',
  'txt',
  'md',
  'log',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
]);

function fileExtensionLower(filename: string): string {
  const i = filename.lastIndexOf('.');
  if (i < 0 || i === filename.length - 1) {
    return '';
  }
  return filename.slice(i + 1).toLowerCase();
}

/**
 * 附件白名单：图片任意格式、mp3/wav 音频、指定扩展名的文本文档。
 * 文档类主要按后缀判断（浏览器常把 Office 标成 application/octet-stream）。
 */
function isAllowedChatAttachment(file: File): boolean {
  const mime = file.type.toLowerCase();
  if (mime.startsWith('image/')) {
    return true;
  }
  const ext = fileExtensionLower(file.name);
  if (ext === 'mp3' || ext === 'wav' || ext === 'm4a') {
    return true;
  }
  return ALLOWED_DOCUMENT_EXTENSIONS.has(ext);
}

function isImageFile(file: File): boolean {
  return file.type.startsWith('image/');
}

function createLocalId(): string {
  return `local-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function handleAttachClick(): void {
  if (props.attachmentMode) {
    fileInputRef.value?.click();
  } else {
    emit('update:attachmentMode', true);
    nextTick(() => fileInputRef.value?.click());
  }
}

async function handleFileInputChange(event: Event): Promise<void> {
  const target = event.target;
  if (!(target instanceof HTMLInputElement) || !target.files) {
    return;
  }

  const picked = Array.from(target.files);
  if (picked.length === 0) {
    return;
  }

  const rejected: string[] = [];
  const files: File[] = [];
  for (const f of picked) {
    if (isAllowedChatAttachment(f)) {
      files.push(f);
    } else {
      rejected.push(f.name);
    }
  }
  if (rejected.length > 0) {
    ElMessage.warning(
      `仅支持图片、MP3/MP4/WAV 音频及 ${[...ALLOWED_DOCUMENT_EXTENSIONS].join(' / ')} 等文档；已跳过：${rejected.join('、')}`,
    );
  }
  if (files.length === 0) {
    target.value = '';
    return;
  }

  // 同种类型文件累加，不同类型则覆盖
  const firstType = files[0].type;
  const isSameType = pendingFiles.value.every((f) => f.file.type === firstType);

  if (!isSameType && pendingFiles.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        `只允许上传同一类型文件，目前已选择 ${pendingFiles.value.length} 个附件，更换类型将清除此前文件，是否继续？`,
        '温馨提示',
        {
          confirmButtonText: '清除并继续',
          cancelButtonText: '取消',
          type: 'warning',
        },
      );
      pendingFiles.value.forEach((f) => URL.revokeObjectURL(f.localUrl));
      pendingFiles.value = [];
    } catch {
      // 用户取消，不做任何操作
      target.value = '';
      return;
    }
  }

  files.forEach((file) => {
    const localId = createLocalId();
    const localUrl = URL.createObjectURL(file);
    pendingFiles.value.push({
      id: localId,
      file,
      localUrl,
    });
  });

  // reset input so same file can be selected again
  target.value = '';

  // 选完文件后聚焦到输入框
  nextTick(() => inputRef.value?.focus());
}

function handleRemoveFile(id: string): void {
  const index = pendingFiles.value.findIndex((f) => f.id === id);
  if (index >= 0) {
    URL.revokeObjectURL(pendingFiles.value[index].localUrl);
    pendingFiles.value.splice(index, 1);
  }
  if (pendingFiles.value.length === 0 && !props.draft.trim()) {
    emit('update:attachmentMode', false);
  }
}

function handleRemoveAllFiles(): void {
  pendingFiles.value.forEach((f) => URL.revokeObjectURL(f.localUrl));
  pendingFiles.value = [];
  if (!props.draft.trim()) {
    emit('update:attachmentMode', false);
  }
}

function handleSend(): void {
  if (props.attachmentMode && hasPendingFiles.value) {
    if (!props.draft.trim()) return;
    const files = [...pendingFiles.value];
    pendingFiles.value = [];
    emit('update:attachmentMode', false);
    emit('send', files);
  } else if (props.canSend) {
    emit('send', []);
  }
}

defineExpose({
  focus,
  adjustHeight,
});
</script>

<template>
  <footer class="composer">
    <!-- 附件模式：输入区与文件列表合并为一个整体 -->
    <div v-if="attachmentMode" class="attachment-panel">
      <div class="input-row">
        <div class="input-box">
          <div class="attachment-input-area">
            <!-- 文件列表：放在 textarea 上方 -->
            <div v-if="hasPendingFiles" class="attachment-items">
              <div class="attachment-items__header">
                <span class="attachment-items__count">已选 {{ pendingFiles.length }} 个文件</span>
                <el-button text size="small" class="attachment-items__clear" @click="handleRemoveAllFiles">清除全部</el-button>
              </div>
              <div v-for="item in pendingFiles" :key="item.id" class="attachment-item">
                <div class="attachment-item__thumb">
                  <img v-if="isImageFile(item.file)" :src="item.localUrl" alt="" class="attachment-thumb-img" />
                  <el-icon v-else class="attachment-thumb-icon"><Paperclip /></el-icon>
                </div>
                <div class="attachment-item__info">
                  <el-tooltip :content="item.file.name" placement="top">
                    <span class="attachment-item__name">{{ item.file.name }}</span>
                  </el-tooltip>
                  <span class="attachment-item__size">{{ formatFileSize(item.file.size) }}</span>
                </div>
                <el-button
                  class="attachment-item__remove"
                  text
                  :icon="Close"
                  @click="handleRemoveFile(item.id)"
                />
              </div>
            </div>
            <div class="attachment-textarea-row">
              <el-input
                ref="inputRef"
                :model-value="draft"
                type="textarea"
                :placeholder="hasPendingFiles ? '请输入文字消息' : composerPlaceholder"
                :disabled="isSending"
                :rows="2"
                class="attachment-textarea"
                @input="(val: string) => emit('update:draft', val)"
                @keydown="handleKeydown"
              />
              <!-- 左侧附件按钮：发送中保持可见，仅禁用避免布局跳动 -->
              <el-tooltip v-if="showAttachmentToggle" :content="isSending ? '回复生成中，请稍后再添加附件' : '添加附件'" placement="top">
                <el-button class="attach-btn-left" text :icon="Paperclip" :disabled="isSending" @click="handleAttachClick" />
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
      <div class="composer-actions">
        <div class="composer-meta">
          <span>当前会话：{{ shortChatId }}</span>
          <span>{{ totalMessageCount }} 条消息</span>
          <span v-if="draftLength > 0">输入中 {{ draftLength }} 字</span>
          <span v-if="requestStatus">{{ requestStatus }}</span>
        </div>
        <div class="action-buttons">
          <el-button
            type="primary"
            class="primary-button"
            :loading="isSending"
            :disabled="!effectiveCanSend"
            @click="handleSend"
          >
            发送
          </el-button>
        </div>
      </div>
      <!-- 隐藏的文件 input -->
      <input
        ref="fileInputRef"
        type="file"
        multiple
        class="hidden-file-input"
        accept="image/*,audio/mpeg,audio/mp3,audio/mp4,audio/wav,audio/x-wav,.mp3,.wav,.pdf,.txt,.md,.log,.doc,.docx,.xls,.xlsx,.ppt,.pptx"
        @change="handleFileInputChange"
      />
    </div>

    <!-- 普通模式 -->
    <div v-else class="composer-shell">
      <div class="input-row">
        <div class="input-box">
          <!-- 左侧附件按钮：发送中保持可见，仅禁用避免布局跳动 -->
          <el-tooltip v-if="showAttachmentToggle" :content="isSending ? '回复生成中，请稍后再添加附件' : '添加附件'" placement="top">
            <el-button class="attach-btn-left" text :icon="Paperclip" :disabled="isSending" @click="handleAttachClick" />
          </el-tooltip>
          <textarea
            ref="inputRef"
            :value="draft"
            class="composer-input"
            :class="{ 'composer-input--no-attach': !showAttachmentToggle }"
            :placeholder="composerPlaceholder"
            :disabled="isSending"
            @input="handleInput"
            @keydown="handleKeydown"
          />
        </div>
      </div>
      <div class="composer-actions">
        <div class="composer-meta">
          <span>当前会话：{{ shortChatId }}</span>
          <span>{{ totalMessageCount }} 条消息</span>
          <span v-if="draftLength > 0">输入中 {{ draftLength }} 字</span>
          <span v-if="requestStatus">{{ requestStatus }}</span>
        </div>
        <div class="action-buttons">
          <el-button v-if="isSending" class="ghost-button" @click="emit('stop')">停止</el-button>
          <el-button
            v-else
            type="primary"
            class="primary-button"
            :disabled="!canSend"
            @click="handleSend"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.composer {
  flex-shrink: 0;
  padding: 10px 0 0;
  background: transparent;
}

/* 通用输入行结构 */
.input-row,
.attachment-input-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-box,
.attachment-input-box {
  position: relative;
  display: flex;
  align-items: flex-start;
}

/* 左侧附件按钮 */
.attach-btn-left {
  position: absolute;
  left: 10px;
  top: 14px;
  z-index: 1;
  width: 28px;
  min-width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease, background 0.15s ease;
}

.attach-btn-left:hover {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}

.attach-btn-left:active,
.attach-btn-left.is-active {
  background: transparent !important;
  color: #2563eb;
}

.attach-btn-left .el-icon {
  font-size: 16px;
}

/* 普通模式 */
.composer-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.84);
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.composer-input {
  width: 100%;
  min-height: 64px;
  max-height: 220px;
  resize: none;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 16px;
  padding: 14px 18px 14px 44px;
  font-size: 15px;
  line-height: 1.75;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.98);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.composer-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.68);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.composer-input--no-attach {
  padding-left: 18px;
}

/* 附件模式 */
.attachment-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.84);
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.attachment-input-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.attachment-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.attachment-input-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.attachment-textarea-row {
  position: relative;
  display: flex;
  align-items: flex-start;
}

.attachment-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  flex-shrink: 0;
}

.attachment-items__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;
}

.attachment-items__count {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.attachment-items__clear {
  font-size: 12px;
  color: #94a3b8;
  padding: 0;
}

.attachment-items__clear:hover {
  color: #ef4444;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.25);
  flex-shrink: 0;
}

.attachment-item__thumb {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  overflow: hidden;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attachment-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.attachment-thumb-icon {
  font-size: 18px;
  color: #64748b;
}

.attachment-item__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.attachment-item__name {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
  display: block;
}

.attachment-item__size {
  font-size: 11px;
  color: #94a3b8;
}

.attachment-item__remove {
  flex-shrink: 0;
  width: 24px;
  min-width: 24px;
  min-height: 24px;
  padding: 0;
  border-radius: 50%;
  color: #94a3b8;
  background: transparent;
  border: none;
}

.attachment-item__remove:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
}

.attachment-textarea {
  --el-input-border-color: rgba(148, 163, 184, 0.3);
  --el-input-focus-border-color: rgba(59, 130, 246, 0.68);
  --el-input-bg-color: rgba(255, 255, 255, 0.98);
  border-radius: 14px;
  overflow: hidden;
}

.attachment-textarea :deep(.el-textarea__inner) {
  border-radius: 14px;
  resize: none;
  font-size: 14px;
  line-height: 1.65;
  padding: 14px 18px 14px 44px;
}

/* 操作栏 */
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.composer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.primary-button,
.ghost-button {
  min-height: 40px;
  border-radius: 11px;
  padding: 10px 16px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  color: #fff;
  background: linear-gradient(140deg, #2563eb, #1d4ed8);
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.22);
}

.primary-button:disabled,
.primary-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-button {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.98);
  border-color: rgba(148, 163, 184, 0.4);
}

.ghost-button:not(.is-disabled):hover {
  border-color: rgba(59, 130, 246, 0.45);
  background: rgba(239, 246, 255, 0.95);
}

.primary-button:not(.is-disabled):hover {
  filter: brightness(1.03);
}

.ghost-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hidden-file-input {
  display: none;
}

@media (max-width: 1024px) {
  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .composer {
    padding: 10px 0 0;
  }
}
</style>
