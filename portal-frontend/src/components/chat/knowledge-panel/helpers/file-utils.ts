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

/** 与后端 `FileExtensionMimeTypeUtils` 中 `EXTENSION_TO_MIME` 静态表一致（仅映射表，不含 JDK 二次猜测）。 */
const EXTENSION_TO_MIME: Readonly<Record<string, string>> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  gif: 'image/gif',
  webp: 'image/webp',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
  ico: 'image/x-icon',
  tif: 'image/tiff',
  tiff: 'image/tiff',
  heic: 'image/heic',
  avif: 'image/avif',
  mp3: 'audio/mp3',
  wav: 'audio/wav',
  ogg: 'audio/ogg',
  oga: 'audio/ogg',
  m4a: 'audio/mp4',
  aac: 'audio/aac',
  flac: 'audio/flac',
  opus: 'audio/opus',
  mp4: 'video/mp4',
  webm: 'video/webm',
  ogv: 'video/ogg',
  mov: 'video/quicktime',
  avi: 'video/x-msvideo',
  mkv: 'video/x-matroska',
  m4v: 'video/x-m4v',
  pdf: 'application/pdf',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  csv: 'text/csv',
  md: 'text/markdown',
  markdown: 'text/markdown',
  txt: 'text/plain',
  html: 'text/html',
  htm: 'text/html',
  css: 'text/css',
  js: 'text/javascript',
  mjs: 'text/javascript',
  json: 'application/json',
  jsonl: 'application/x-ndjson',
  xml: 'application/xml',
  yaml: 'application/yaml',
  yml: 'application/yaml',
  zip: 'application/zip',
  rar: 'application/vnd.rar',
  '7z': 'application/x-7z-compressed',
  tar: 'application/x-tar',
  gz: 'application/gzip',
};

/** 与 {@code FileExtensionMimeTypeUtils#normalizeExtension} 对齐：取路径最后一段、再取最后一个 `.` 后的小写后缀 */
function normalizeExtension(filenameOrExtension: string): string {
  const raw = filenameOrExtension.trim();
  if (!raw) {
    return '';
  }
  let s = raw;
  const slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
  if (slash >= 0 && slash < s.length - 1) {
    s = s.slice(slash + 1);
  }
  const dot = s.lastIndexOf('.');
  if (dot < 0 || dot === s.length - 1) {
    if (s.startsWith('.') && s.length > 1) {
      return s.slice(1).toLowerCase();
    }
    return s.toLowerCase();
  }
  return s.slice(dot + 1).toLowerCase();
}

/**
 * 按后缀从 `EXTENSION_TO_MIME` 推断 MIME；未知后缀返回 `undefined`（由调用方兜底 `application/octet-stream`）。
 * 入参可为完整路径、文件名或单独扩展名（与后端 `getMimeTypeFromExtension` 映射阶段一致）。
 */
export function guessMimeFromFilename(filenameOrExtension: string): string | undefined {
  const ext = normalizeExtension(filenameOrExtension);
  if (!ext) {
    return undefined;
  }
  return EXTENSION_TO_MIME[ext];
}

export function effectiveContentType(file: ChatFileListItem): string {
  const fromMeta = file.contentType?.trim();
  if (fromMeta) {
    return fromMeta;
  }
  return guessMimeFromFilename(file.originalFilename) ?? 'application/octet-stream';
}
