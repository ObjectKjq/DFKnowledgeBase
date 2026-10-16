package com.df.knowledge.common;

/**
 * 全局错误码常量
 */
public interface GlobalErrorCodeConstants {

    ErrorCode SUCCESS = new ErrorCodeImpl(0, "success");

    ErrorCode BAD_REQUEST = new ErrorCodeImpl(400, "请求参数不正确");

    ErrorCode UNAUTHORIZED = new ErrorCodeImpl(401, "账号未登录");

    ErrorCode FORBIDDEN = new ErrorCodeImpl(403, "没有权限访问");

    ErrorCode NOT_FOUND = new ErrorCodeImpl(404, "请求不存在");

    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCodeImpl(500, "系统异常");

    ErrorCode UNKNOWN = new ErrorCodeImpl(9999, "未知异常");
}
