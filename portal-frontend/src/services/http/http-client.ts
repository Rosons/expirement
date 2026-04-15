import axios, { type AxiosError } from 'axios';
import type { ApiResponse } from '../../types/chat';
import { notifyRequestFailure } from '../notifications/error-toast-service';
import { getErrorMessage } from '../../utils/error-message';

function isApiEnvelope(value: unknown): value is ApiResponse<unknown> {
  return value !== null && typeof value === 'object' && typeof (value as ApiResponse<unknown>).code === 'number';
}

function unwrapSuccessBody(body: unknown): unknown {
  if (!isApiEnvelope(body)) {
    throw new Error('响应格式异常：缺少约定字段 code');
  }
  if (body.code !== 0) {
    throw new Error(body.message || `业务错误 code=${body.code}`);
  }
  return body.data;
}

/** 与后端 `ApiResponse` 对齐；成功时 `response.data` 为解包后的载荷。流式请仍用 `fetch`。 */
export const apiClient = axios.create({
  headers: {
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache',
  },
});

apiClient.interceptors.response.use(
  (response) => {
    try {
      const unwrapped = unwrapSuccessBody(response.data);
      return { ...response, data: unwrapped };
    } catch (e) {
      const err = e instanceof Error ? e : new Error(getErrorMessage(e));
      if (!response.config.skipGlobalErrorToast) {
        notifyRequestFailure(err);
      }
      return Promise.reject(err);
    }
  },
  (error: AxiosError) => {
    const raw = error.response?.data;
    let err: Error;
    if (isApiEnvelope(raw)) {
      err = new Error(raw.message || `业务错误 code=${raw.code}`);
    } else if (typeof raw === 'string' && raw.trim()) {
      err = new Error(raw.trim().slice(0, 240));
    } else {
      const status = error.response?.status;
      err = status ? new Error(`请求失败（HTTP ${status}）`) : new Error(error.message || '网络异常');
    }
    if (!error.config?.skipGlobalErrorToast) {
      notifyRequestFailure(err);
    }
    return Promise.reject(err);
  },
);
