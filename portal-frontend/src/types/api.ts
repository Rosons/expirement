/** 与后端 ApiResponse 对齐 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
