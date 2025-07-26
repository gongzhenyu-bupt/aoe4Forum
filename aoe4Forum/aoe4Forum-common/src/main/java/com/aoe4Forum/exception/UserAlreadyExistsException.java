package com.aoe4Forum.exception;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String message) {
        super(409,message+"已存在");
    }

}
