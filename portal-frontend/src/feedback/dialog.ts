import { ElMessageBox } from 'element-plus';

interface ConfirmDangerOptions {
  title?: string;
  confirmText?: string;
  cancelText?: string;
}

export async function confirmDanger(message: string, options?: ConfirmDangerOptions): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, options?.title ?? '确认操作', {
      confirmButtonText: options?.confirmText ?? '确定',
      cancelButtonText: options?.cancelText ?? '取消',
      type: 'warning',
      autofocus: false,
    });
    return true;
  } catch {
    return false;
  }
}
