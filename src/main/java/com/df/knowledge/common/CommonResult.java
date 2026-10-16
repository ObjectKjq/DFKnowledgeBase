package com.df.knowledge.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用返回值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult<T> implements Serializable {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    public static <T> CommonResult<T> success() {
        return success(null);
    }

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(GlobalErrorCodeConstants.SUCCESS.getCode());
        result.setMsg(GlobalErrorCodeConstants.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /**
     * 是否成功（不参与 JSON 序列化）
     */
    @JsonIgnore
    public boolean isSuccess() {
        return GlobalErrorCodeConstants.SUCCESS.getCode() == getCode();
    }
}
