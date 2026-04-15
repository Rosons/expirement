import { reactive } from 'vue';
import { getErrorMessage } from '../../utils/error-message';

export const globalToastState = reactive({
  visible: false,
  text: '',
});

let hideTimer: ReturnType<typeof setTimeout> | null = null;

export function showGlobalError(message: string): void {
  if (hideTimer) {
    clearTimeout(hideTimer);
    hideTimer = null;
  }
  globalToastState.text = message;
  globalToastState.visible = true;
  hideTimer = setTimeout(() => {
    globalToastState.visible = false;
    hideTimer = null;
  }, 4500);
}

/** 请求失败时弹出全局提示；`silent` 用于用户取消等不应打扰的场景 */
export function notifyRequestFailure(error: unknown, options?: { silent?: boolean }): void {
  if (options?.silent) {
    return;
  }
  showGlobalError(getErrorMessage(error));
}
