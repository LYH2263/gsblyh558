package com.exam.system.exception;

import lombok.Getter;

/**
 * 场次预约业务异常。携带 RSV_ 语义错误码，由全局异常处理器统一包装进 ApiResponse。
 */
@Getter
public class ReservationException extends RuntimeException {

    private final ReservationErrorCode errorCode;

    public ReservationException(ReservationErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}
