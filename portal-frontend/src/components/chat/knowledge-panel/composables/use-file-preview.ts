import { ref } from 'vue';
import type { ChatFileListItem } from '../../../../types/chat-file';
import { effectiveContentType } from '../helpers/file-utils';

type PreviewKind = 'image' | 'pdf' | 'text' | 'unsupported';

export function useFilePreview(downloadById: (fileId: string) => Promise<Blob>) {
  const previewOpen = ref(false);
  const previewTitle = ref('');
  const previewKind = ref<PreviewKind>('unsupported');
  const previewText = ref('');
  const previewImageUrl = ref('');
  const previewPdfUrl = ref('');
  let previewObjectUrl: string | null = null;

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

  async function handleDownload(file: ChatFileListItem): Promise<void> {
    const blob = await downloadById(file.fileId);
    const mime = effectiveContentType(file);
    const typed = blob.type && blob.type !== 'application/octet-stream' ? blob : new Blob([blob], { type: mime });
    const url = URL.createObjectURL(typed);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = file.originalFilename || 'download';
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2_000);
  }

  async function handlePreview(file: ChatFileListItem): Promise<void> {
    closePreview();
    previewTitle.value = file.originalFilename;
    const blob = await downloadById(file.fileId);
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
