import { ElMessage } from 'element-plus';
import { getErrorMessage } from '../utils/error-message';

export function showGlobalError(message: string): void {
  ElMessage.closeAll();
  ElMessage({
    type: 'error',
    message,
    duration: 4500,
    showClose: true,
    grouping: true,
  });
}

/** 请求失败时弹出全局提示；`silent` 用于用户取消等不应打扰的场景 */
export function notifyRequestFailure(error: unknown, options?: { silent?: boolean }): void {
  if (options?.silent) {
    return;
  }
  showGlobalError(getErrorMessage(error));
}
