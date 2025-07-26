package com.aoe4Forum.entity;

import lombok.Data;

@Data
public class ResponseVO<T> {

    private String code;
    private String message;
    private T data;

    public ResponseVO() {}

//    有参构造
    public ResponseVO(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

//请求成功时
    public static <T> ResponseVO<T> success(String message,T data){
        return new ResponseVO<T>("0",message,data);
    }

    public static <T> ResponseVO<T> success() {
        return new ResponseVO<>("0", "操作成功", null);
    }
//    请求失败时
    public static <T> ResponseVO<T> error(String code, String message) {
        return new ResponseVO<>(code, message, null);
    }
//

}
