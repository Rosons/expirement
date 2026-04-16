import type { ChatFileListItem } from '../../../../types/chat-file';

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value < 10 && unitIndex > 0 ? value.toFixed(1) : Math.round(value)} ${units[unitIndex]}`;
}

export function formatShortTime(iso: string | null | undefined): string {
  if (!iso?.trim()) {
    return '';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return date.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function fileExtension(name: string): string {
  const index = name.lastIndexOf('.');
  if (index < 0 || index === name.length - 1) {
    return '';
  }
  return name.slice(index + 1).toUpperCase().slice(0, 4);
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

export function effectiveContentType(file: ChatFileListItem): string {
  const fromMeta = file.contentType?.trim();
  if (fromMeta) {
    return fromMeta;
  }
  return guessMimeFromFilename(file.originalFilename) ?? 'application/octet-stream';
}
