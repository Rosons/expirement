/** 前端 API 相关环境变量（单一入口，业务代码勿直接读 import.meta.env） */

export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE as string | undefined)?.trim().replace(/\/+$/, '') ?? '';

const CHAT_API_VERSION_RAW =
  (import.meta.env.VITE_CHAT_API_VERSION as string | undefined)?.trim() ?? 'v1';

export const CHAT_API_VERSION: 'v1' | 'v2' =
  CHAT_API_VERSION_RAW.toLowerCase() === 'v2' ? 'v2' : 'v1';

const HISTORY_PAGE = Math.max(1, Number(import.meta.env.VITE_CHAT_HISTORY_PAGE ?? '1'));
const HISTORY_SIZE = Math.min(200, Math.max(1, Number(import.meta.env.VITE_CHAT_HISTORY_PAGE_SIZE ?? '10')));

/** 与 .env 一致，供分页默认值 */
export const CHAT_HISTORY_DEFAULT_PAGE = HISTORY_PAGE;
export const CHAT_HISTORY_PAGE_SIZE = HISTORY_SIZE;

export function getChatApiVersion(): 'v1' | 'v2' {
  return CHAT_API_VERSION;
}
