package com.gmp.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.LoginDTO;
import com.gmp.dto.LoginVO;
import com.gmp.service.AuthService;
import com.gmp.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证增强Service实现类
 * <p>
 * 安全机制：
 * 1. 图形验证码：登录前必须校验，防止脚本暴力破解；验证码一次性使用，5分钟过期；
 * 2. 登录失败锁定：同一用户名连续失败 5 次后锁定 10 分钟，防止撞库攻击。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 验证码Redis前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    /** 验证码有效期（分钟） */
    private static final long CAPTCHA_TTL_MINUTES = 5;
    /** 登录失败计数Redis前缀 */
    private static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    /** 连续失败最大次数 */
    private static final int MAX_FAIL_TIMES = 5;
    /** 锁定时长（分钟） */
    private static final long LOCK_TTL_MINUTES = 10;

    private final SysUserService userService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<String, String> generateCaptcha() {
        // 生成4位数字图形验证码（宽150高50）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(150, 50, 4, 100);

        // 生成唯一key并存入Redis（小写存储便于比对）
        String key = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + key,
                captcha.getCode().toLowerCase(),
                Duration.ofMinutes(CAPTCHA_TTL_MINUTES)
        );

        Map<String, String> result = new HashMap<>(2);
        result.put("captchaKey", key);
        result.put("captchaImage", captcha.getImageBase64Data());
        return result;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 校验验证码
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());

        // 2. 检查账号是否已被锁定
        String failKey = LOGIN_FAIL_PREFIX + dto.getUsername();
        checkNotLocked(failKey);

        // 3. 执行账号密码登录
        try {
            LoginVO loginVO = userService.login(dto.getUsername(), dto.getPassword());
            // 登录成功，清除失败计数
            redisTemplate.delete(failKey);
            return loginVO;
        } catch (BusinessException e) {
            // 仅对"用户名或密码错误"累计失败次数，锁定输入错误类异常不计
            if (e.getCode() == ErrorCode.LOGIN_FAILED) {
                recordFail(failKey);
            }
            throw e;
        }
    }

    /**
     * 校验图形验证码（一次性使用，无论对错均删除）
     */
    private void verifyCaptcha(String captchaKey, String captchaCode) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "请输入验证码");
        }
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String expected = redisTemplate.opsForValue().get(redisKey);
        // 无论校验结果如何，验证码均失效（一次性）
        redisTemplate.delete(redisKey);
        if (expected == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "验证码已过期，请刷新后重试");
        }
        if (!expected.equals(captchaCode.trim().toLowerCase())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "验证码错误");
        }
    }

    /**
     * 检查账号是否处于锁定状态
     */
    private void checkNotLocked(String failKey) {
        String times = redisTemplate.opsForValue().get(failKey);
        if (times != null && Integer.parseInt(times) >= MAX_FAIL_TIMES) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "登录失败次数过多，账号已锁定" + LOCK_TTL_MINUTES + "分钟，请稍后再试");
        }
    }

    /**
     * 记录一次登录失败，达到阈值即锁定
     */
    private void recordFail(String failKey) {
        Long times = redisTemplate.opsForValue().increment(failKey);
        if (times != null && times >= MAX_FAIL_TIMES) {
            log.warn("账号 {} 连续登录失败 {} 次，已锁定 {} 分钟", failKey, times, LOCK_TTL_MINUTES);
            // 达到阈值后从锁定时刻重新计时
            redisTemplate.expire(failKey, Duration.ofMinutes(LOCK_TTL_MINUTES));
        } else if (times != null && times == 1L) {
            // 首次失败开始计时（10分钟内失败次数累计）
            redisTemplate.expire(failKey, Duration.ofMinutes(LOCK_TTL_MINUTES));
        }
    }
}
