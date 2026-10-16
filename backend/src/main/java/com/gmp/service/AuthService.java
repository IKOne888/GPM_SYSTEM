package com.gmp.service;

import com.gmp.dto.LoginDTO;
import com.gmp.dto.LoginVO;

import java.util.Map;

/**
 * 认证增强Service接口
 * <p>
 * 负责登录前的安全校验：图形验证码校验、登录失败次数锁定。
 */
public interface AuthService {

    /**
     * 生成图形验证码
     *
     * @return 验证码key与base64图片
     */
    Map<String, String> generateCaptcha();

    /**
     * 带验证码与失败锁定的登录
     *
     * @param dto 登录请求（含验证码）
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto);
}
