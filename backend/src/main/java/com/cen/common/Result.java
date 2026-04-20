package com.cen.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
* 接口返回包装类
* */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public static  Result success(){
        return new Result(Constants.CODE_200,"ok",null);
    }
    public static  Result success(Object data){
        return new Result(Constants.CODE_200,"ok",data);
    }
    public static  Result error(Integer code,String msg){
        return new Result(code,msg,null);
    }
    public static  Result error(){
        return new Result(Constants.CODE_500,"系统异常错误",null);
    }
}
