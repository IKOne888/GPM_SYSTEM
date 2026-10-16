package com.gmp.common;

/**
 * 错误码常量类
 * <p>
 * 统一管理业务错误码，避免魔法数字散落各处。
 * 约定：1000 段为系统级错误，2000 段为用户/认证，3000 段为业务模块错误。
 */
public class ErrorCode {

    /** 操作成功 */
    public static final int SUCCESS = 200;
    /** 参数校验失败 */
    public static final int PARAM_ERROR = 400;
    /** 未登录或登录已过期 */
    public static final int UNAUTHORIZED = 401;
    /** 没有权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 系统内部错误 */
    public static final int SYSTEM_ERROR = 500;

    /** 用户名或密码错误 */
    public static final int LOGIN_FAILED = 2001;
    /** 账号已被禁用 */
    public static final int ACCOUNT_DISABLED = 2002;
    /** 用户名已存在 */
    public static final int USERNAME_EXISTS = 2003;
    /** 数据已存在（唯一键冲突） */
    public static final int DUPLICATE_KEY = 2004;
    /** 验证码错误或过期 */
    public static final int CAPTCHA_ERROR = 2005;
    /** 账号登录失败次数过多已锁定 */
    public static final int ACCOUNT_LOCKED = 2006;
    /** 不能操作自己的账号 */
    public static final int CANNOT_OPERATE_SELF = 2007;
    /** 角色等级不足，无权管理/分配该角色 */
    public static final int ROLE_LEVEL_DENIED = 2008;
    /** 文件类型不支持 */
    public static final int FILE_TYPE_UNSUPPORTED = 3001;
    /** 文件大小超限 */
    public static final int FILE_SIZE_EXCEEDED = 3002;
    /** 数据校验失败（业务规则） */
    public static final int BUSINESS_ERROR = 3003;

    private ErrorCode() {
    }
}
