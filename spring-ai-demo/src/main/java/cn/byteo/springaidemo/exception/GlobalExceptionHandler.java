package cn.byteo.springaidemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file GlobalExceptionHandler
 * @since 2026/4/14 18:40
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception e) {
        // 这里可以根据不同的异常类型进行不同的处理
        return createErrorResponse(e.getMessage());
    }


    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
                "success", false,
                "error", message
        );
    }
}
