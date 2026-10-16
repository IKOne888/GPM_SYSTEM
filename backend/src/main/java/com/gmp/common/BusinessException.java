package com.gmp.common;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * Service 层抛出业务异常时统一使用本类，由全局异常处理器转译为
 * {@link Result} 返回前端，避免将内部实现细节泄露给客户端。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final int code;

    /**
     * 默认使用通用业务错误码
     *
     * @param message 错误提示信息（会原样返回给前端，请使用友好文案）
     */
    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }

    /**
     * 指定错误码
     *
     * @param code    错误码
     * @param message 错误提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
