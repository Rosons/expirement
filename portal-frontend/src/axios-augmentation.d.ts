import type {} from 'axios';

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 为 true 时不弹出全局错误 Toast（仍 reject） */
    skipGlobalErrorToast?: boolean;
  }
}
