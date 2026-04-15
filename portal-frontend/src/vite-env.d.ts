/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string;
  /** 聊天接口版本：v1（内存 + Spring Message 形态历史）或 v2（数据库 + 分页 +自有 VO） */
  readonly VITE_CHAT_API_VERSION?: string;
  /** v1 / v2 历史查询页码，默认 1 */
  readonly VITE_CHAT_HISTORY_PAGE?: string;
  /** v1 / v2 历史每页条数，默认 100，最大 200 */
  readonly VITE_CHAT_HISTORY_PAGE_SIZE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
