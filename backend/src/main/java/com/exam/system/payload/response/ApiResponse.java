package com.exam.system.payload.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int code;
    /** 业务错误码（如 RSV_SESSION_FULL），成功时为空 */
    private String errorCode;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public ApiResponse(boolean success, String message, T data, int code, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.errorCode = errorCode;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, 200);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, 200);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, message, null, code);
    }
    
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(false, message, data, code);
    }

    public static <T> ApiResponse<T> error(int code, String errorCode, String message) {
        return new ApiResponse<>(false, message, null, code, errorCode);
    }
}
