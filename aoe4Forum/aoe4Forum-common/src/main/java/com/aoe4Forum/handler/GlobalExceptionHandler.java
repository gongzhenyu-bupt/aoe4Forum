package com.aoe4Forum.handler;


import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.exception.BusinessException;
import com.aoe4Forum.exception.ErrorParamsException;
import com.aoe4Forum.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseVO<String> handleConstraintViolationException(ConstraintViolationException ex){
        return ResponseVO.error("10000","参数错误");
    }

    @ExceptionHandler({UserAlreadyExistsException.class,ErrorParamsException.class, BusinessException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseVO<String> handleUserAlreadyExistsException(Exception ex){

        return ResponseVO.error("10001",ex.getMessage());
    }

}
