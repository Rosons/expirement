package cn.byteo.springaidemo.common.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 HTTP JSON 响应（业务成功 {@code code == 0}，{@code data} 为载荷）。
 * <p>流式接口（如 SSE）不使用本包装。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 0 表示成功，非 0 为错误码 */
    private int code;

    private String message;

    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().code(0).message("ok").data(data).build();
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).data(null).build();
    }

    public static ApiResponse<Void> failVoid(int code, String message) {
        return ApiResponse.<Void>builder().code(code).message(message).data(null).build();
    }
}
