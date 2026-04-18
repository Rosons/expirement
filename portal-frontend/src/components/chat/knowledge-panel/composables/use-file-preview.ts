import { ref } from 'vue';
import { getChatFileDownloadUrl, getChatFilePreviewUrl } from '../../../../api/chat-endpoints';
import { fetchChatFileText } from '../../../../services/chat/chat-file-service';
import type { ChatFileListItem } from '../../../../types/chat-file';
import { effectiveContentType } from '../helpers/file-utils';

type PreviewKind = 'image' | 'pdf' | 'text' | 'unsupported';

/**
 * 下载用 attachment 直链；预览用 `?inline=1`（后端 inline + 正确 Content-Type），避免 img/iframe 被当成附件触发下载。
 * 文本预览仍 fetch 为字符串。
 */
export function useFilePreview() {
  const previewOpen = ref(false);
  const previewTitle = ref('');
  const previewKind = ref<PreviewKind>('unsupported');
  const previewText = ref('');
  const previewImageUrl = ref('');
  const previewPdfUrl = ref('');

  function revokePreviewUrl(): void {
    previewImageUrl.value = '';
    previewPdfUrl.value = '';
    previewText.value = '';
  }

  function closePreview(): void {
    previewOpen.value = false;
    revokePreviewUrl();
  }

  function handleDownload(file: ChatFileListItem): void {
    const url = getChatFileDownloadUrl(file.fileId);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = file.originalFilename || 'download';
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  }

  async function handlePreview(file: ChatFileListItem): Promise<void> {
    closePreview();
    previewTitle.value = file.originalFilename;
    const mime = effectiveContentType(file);
    const previewUrl = getChatFilePreviewUrl(file.fileId);

    if (mime.startsWith('image/')) {
      previewKind.value = 'image';
      previewImageUrl.value = previewUrl;
      previewOpen.value = true;
      return;
    }

    if (mime === 'application/pdf' || file.originalFilename.toLowerCase().endsWith('.pdf')) {
      previewKind.value = 'pdf';
      previewPdfUrl.value = previewUrl;
      previewOpen.value = true;
      return;
    }

    if (mime.startsWith('text/') || mime === 'application/json' || mime === 'application/csv') {
      previewKind.value = 'text';
      previewText.value = await fetchChatFileText(file.fileId);
      previewOpen.value = true;
      return;
    }

    previewKind.value = 'unsupported';
    previewOpen.value = true;
  }

  return {
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
  };
}
