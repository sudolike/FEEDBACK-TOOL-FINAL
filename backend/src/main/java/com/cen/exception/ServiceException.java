package com.cen.exception;


import lombok.Getter;

/*
* 自定义异常
* */
@Getter
public class ServiceException extends RuntimeException {
    private Integer code;

    public ServiceException(Integer code, String msg){
        super(msg);
        this.code = code;
    }
}
