package com.exam.system.payload.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int code;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
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
}
