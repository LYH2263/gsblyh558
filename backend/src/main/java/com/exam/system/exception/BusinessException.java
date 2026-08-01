package com.exam.system.exception;

import lombok.Getter;

/**
 * 业务异常：携带 RSV_ 业务错误码与 HTTP 状态码，
 * 由 GlobalExceptionHandler 统一转换为 ApiResponse 响应体。
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public BusinessException(int status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
