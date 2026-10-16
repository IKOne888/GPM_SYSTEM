package com.gmp.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.gmp.entity.SysAuditLog;
import com.gmp.entity.SysUser;
import com.gmp.mapper.SysAuditLogMapper;
import com.gmp.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 审计追踪AOP切面
 * 自动记录所有业务操作日志
 * <p>
 * 安全说明：
 * 1. 仅记录写操作（POST/PUT/DELETE），避免查询日志导致表无限膨胀；
 * 2. 请求参数中的敏感字段（password、token 等）做脱敏处理，防止明文密码落库；
 * 3. 客户端IP 从 X-Forwarded-For 取首个非空值，避免伪造或误取。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 敏感字段名集合（序列化参数时自动脱敏） */
    private static final String[] SENSITIVE_KEYS = {"password", "newPassword", "token", "secret", "Authorization"};

    /** 仅记录写操作，查询类方法不落审计表 */
    private static final String[] WRITE_METHODS = {"add", "create", "insert", "update", "edit", "modify", "delete", "remove", "save", "upload", "logout"};

    private final SysAuditLogMapper auditLogMapper;
    private final SysUserMapper userMapper;

    @Pointcut("execution(* com.gmp.controller..*.*(..))")
    public void auditPointcut() {
    }

    @Around("auditPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String methodName = point.getSignature().getName();
        // 查询类操作不记录审计日志，避免表无限膨胀
        if (!isWriteOperation(methodName)) {
            return point.proceed();
        }

        long startTime = System.currentTimeMillis();
        SysAuditLog auditLog = new SysAuditLog();

        try {
            // 获取当前登录用户
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    auditLog.setUserId(userId);
                    auditLog.setUsername(user.getUsername());
                    auditLog.setRealName(user.getRealName());
                }
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURL().toString());
                auditLog.setIp(getIpAddress(request));
                auditLog.setUserAgent(StrUtil.sub(request.getHeader("User-Agent"), 0, 500));
            }

            // 获取请求参数（脱敏后记录）
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                String requestParams = JSONUtil.toJsonStr(args);
                String masked = maskSensitive(requestParams);
                auditLog.setRequestParams(StrUtil.sub(masked, 0, 2000));
            }

            // 设置操作类型
            auditLog.setOperationType(getOperationType(methodName));
            auditLog.setModule(point.getTarget().getClass().getSimpleName());
            auditLog.setDescription(methodName);

        } catch (Exception e) {
            log.error("审计日志记录异常：{}", e.getMessage());
        }

        // 执行目标方法
        Object result = null;
        try {
            result = point.proceed();
            auditLog.setStatus(1);
        } catch (Throwable e) {
            auditLog.setStatus(0);
            auditLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
            throw e;
        } finally {
            // 保存审计日志
            auditLog.setExecuteTime((int) (System.currentTimeMillis() - startTime));
            auditLog.setOperationTime(LocalDateTime.now());
            try {
                auditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("保存审计日志失败：{}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 是否写操作（POST/PUT/DELETE 类方法）
     */
    private boolean isWriteOperation(String methodName) {
        for (String prefix : WRITE_METHODS) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对 JSON 字符串中的敏感字段值脱敏，防止明文密码/令牌落库
     * 例如："password":"123456" -> "password":"******"
     */
    private String maskSensitive(String json) {
        String result = json;
        for (String key : SENSITIVE_KEYS) {
            result = result.replaceAll("(?i)(\"" + key + "\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3");
        }
        return result;
    }

    private String getOperationType(String methodName) {
        if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("insert")
                || methodName.startsWith("save") || methodName.startsWith("upload")) {
            return "INSERT";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return "QUERY";
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        // 取 X-Forwarded-For 链中的第一个IP（客户端原始IP），多级代理时最左侧为真实客户端
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            String firstIp = ip.split(",")[0].trim();
            if (!"unknown".equalsIgnoreCase(firstIp)) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }
}
