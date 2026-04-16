package cn.byteo.springaidemo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常，支持业务码与 HTTP 状态码。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), message);
    }

    public BusinessException(HttpStatus httpStatus, String message) {
        this(httpStatus, httpStatus.value(), message);
    }

    public BusinessException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

}
