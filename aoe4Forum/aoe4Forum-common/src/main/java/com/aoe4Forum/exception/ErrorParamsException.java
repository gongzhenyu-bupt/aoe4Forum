package com.aoe4Forum.exception;

public class ErrorParamsException extends BusinessException {
    public ErrorParamsException(String message) {
        super(401,message+"错误");
    }
}
