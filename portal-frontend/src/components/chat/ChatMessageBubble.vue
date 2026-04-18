<script setup lang="ts">
import { CopyDocument, RefreshRight, Picture, Document, VideoCamera, Headset } from '@element-plus/icons-vue';
import type { ChatRole } from '../../types/chat';
import type { ChatMessagePartVo } from '../../types/chat';
import { useMessageBubble } from './message-bubble';

const props = withDefaults(
  defineProps<{
    messageId: string;
    role: ChatRole;
    content: string;
    createdAt: number;
    streaming?: boolean;
    enableCopyAction?: boolean;
    enableResendAction?: boolean;
    parts?: ChatMessagePartVo[];
  }>(),
  {
    enableCopyAction: true,
    enableResendAction: true,
    parts: () => [],
  },
);
const emit = defineEmits<{
  resendMessage: [content: string];
}>();

const {
  renderedHtml,
  canCopyMessage,
  canResendMessage,
  roleLabel,
  avatarLabel,
  formattedTime,
  showAssistantStreamingPlaceholder,
  handleMarkdownClick,
  handleCopyMessage,
  handleResendMessage,
} = useMessageBubble(props, (content) => {
  emit('resendMessage', content);
});

function isImagePart(part: ChatMessagePartVo): boolean {
  const mime = part.mimeType ?? '';
  return (
    mime.startsWith('image/') || Boolean(part.mediaUrl && /\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i.test(part.mediaUrl))
  );
}

function isVideoPart(part: ChatMessagePartVo): boolean {
  const mime = part.mimeType ?? '';
  return (
    mime.startsWith('video/') || Boolean(part.mediaUrl && /\.(mp4|mov|avi|mkv|webm)$/i.test(part.mediaUrl))
  );
}

function isAudioPart(part: ChatMessagePartVo): boolean {
  const mime = part.mimeType ?? '';
  return (
    mime.startsWith('audio/') || Boolean(part.mediaUrl && /\.(mp3|wav|flac|m4a|aac)$/i.test(part.mediaUrl))
  );
}

function getPartIcon(part: ChatMessagePartVo) {
  if (isImagePart(part)) return Picture;
  if (isVideoPart(part)) return VideoCamera;
  if (isAudioPart(part)) return Headset;
  return Document;
}

function getPartLabel(part: ChatMessagePartVo): string {
  if (isImagePart(part)) return '图片';
  if (isVideoPart(part)) return '视频';
  if (isAudioPart(part)) return '音频';
  return '文件';
}

function getPartSrc(part: ChatMessagePartVo): string {
  return part.mediaUrl ?? part.contentText ?? '';
}
</script>

<template>
  <article class="message-row" :class="`message-row--${props.role}`">
    <div class="avatar-shell">
      <div class="avatar">
        {{ avatarLabel }}
      </div>
    </div>
    <div class="bubble-wrap">
      <header class="bubble-meta">
        <div class="bubble-meta__main">
          <span class="bubble-role">{{ roleLabel }}</span>
          <span v-if="formattedTime" class="bubble-time">{{ formattedTime }}</span>
        </div>
      </header>

      <!-- 附件区域：图片等媒体附件在上方 -->
      <div v-if="props.parts && props.parts.length > 0" class="bubble-parts">
        <template v-for="part in props.parts" :key="part.partIndex">
          <!-- 图片 -->
          <div v-if="isImagePart(part)" class="part-image-wrap">
            <el-image
              class="part-image"
              :src="getPartSrc(part)"
              :preview-src-list="[getPartSrc(part)]"
              fit="cover"
              :preview-teleported="true"
            />
          </div>
          <!-- 视频 -->
          <div v-else-if="isVideoPart(part)" class="part-media-wrap">
            <video class="part-media" :src="getPartSrc(part)" controls />
          </div>
          <!-- 音频 -->
          <div v-else-if="isAudioPart(part)" class="part-media-wrap">
            <audio class="part-media" :src="getPartSrc(part)" controls />
          </div>
          <!-- 其他文件 -->
          <div v-else class="part-file">
            <el-icon class="part-file__icon"><component :is="getPartIcon(part)" /></el-icon>
            <span class="part-file__label">{{ getPartLabel(part) }}</span>
            <a v-if="getPartSrc(part)" :href="getPartSrc(part)" target="_blank" class="part-file__link" download>
              下载
            </a>
          </div>
        </template>
      </div>

      <div class="bubble">
        <span v-if="showAssistantStreamingPlaceholder" class="streaming-hint">正在回复中…</span>
        <div
          v-if="!showAssistantStreamingPlaceholder"
          class="markdown-body"
          :key="`${props.messageId}-${props.content.length}-${props.streaming ? 1 : 0}`"
          v-html="renderedHtml"
          @click="handleMarkdownClick"
        />
        <span v-if="props.streaming && !showAssistantStreamingPlaceholder" class="cursor" />
      </div>
      <div v-if="canCopyMessage || canResendMessage" class="bubble-toolbar">
        <el-tooltip v-if="canCopyMessage" content="复制" placement="top" :show-after="150">
          <el-button
            class="meta-action-button"
            text
            :icon="CopyDocument"
            aria-label="复制"
            @click="handleCopyMessage"
          />
        </el-tooltip>
        <el-tooltip v-if="canResendMessage" content="重发" placement="top" :show-after="150">
          <el-button
            class="meta-action-button"
            text
            :icon="RefreshRight"
            aria-label="重发"
            @click="handleResendMessage"
          />
        </el-tooltip>
      </div>
    </div>
  </article>
</template>

<style scoped src="./message-bubble/styles/chat-message-bubble.css"></style>
