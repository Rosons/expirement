<script setup lang="ts">
type PreviewKind = 'image' | 'pdf' | 'text' | 'unsupported';

defineProps<{
  modelValue: boolean;
  title: string;
  kind: PreviewKind;
  imageUrl: string;
  pdfUrl: string;
  text: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  closed: [];
}>();
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    class="kb-preview-dialog"
    :title="title"
    :width="kind === 'pdf' ? '1120px' : '920px'"
    align-center
    append-to-body
    :modal="true"
    :close-on-click-modal="true"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @closed="emit('closed')"
  >
    <div class="kb-preview__body">
      <img v-if="kind === 'image'" class="kb-preview__img" :src="imageUrl" alt="" />
      <iframe v-else-if="kind === 'pdf'" class="kb-preview__frame" title="PDF" :src="pdfUrl" />
      <pre v-else-if="kind === 'text'" class="kb-preview__pre">{{ text }}</pre>
      <p v-else class="kb-preview__muted">此格式请使用「下载」查看。</p>
    </div>
  </el-dialog>
</template>

<style scoped>
.kb-preview-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 12vh);
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  box-shadow: 0 30px 78px rgba(15, 23, 42, 0.28);
  overflow: hidden;
}

.kb-preview-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.96));
}

.kb-preview-dialog :deep(.el-dialog__title) {
  display: block;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.kb-preview__body {
  min-height: 0;
  max-height: calc(100vh - 28vh);
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
  min-height: 74vh;
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

@media (max-width: 640px) {
  .kb-preview-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    max-height: calc(100vh - 6vh);
  }

  .kb-preview__body {
    max-height: calc(100vh - 20vh);
  }
}
</style>
