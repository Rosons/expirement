package cn.byteo.springaidemo.exception;

import cn.byteo.springaidemo.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：与 {@link ApiResponse} 一致，便于前端统一解析。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Business request failed";
        return ApiResponse.failVoid(e.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
        return ApiResponse.failVoid(500, msg);
    }
}
