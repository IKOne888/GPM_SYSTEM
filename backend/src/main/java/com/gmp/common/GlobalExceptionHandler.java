package com.gmp.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获各层异常并转译为标准 {@link Result} 返回。
 * 安全原则：内部实现细节（SQL、堆栈、类名）只写入日志，绝不回显给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（Service 层主动抛出，message 为友好提示，直接返回）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常[{}]：{}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 未登录
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录或登录已过期：{}", e.getMessage());
        return Result.error(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
    }

    /**
     * 无权限
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        // 将缺失的权限码作为拒绝原因返回，便于前端提示与排查
        String permission = e.getPermission();
        log.warn("无操作权限，需要权限：{}", permission);
        return Result.error(ErrorCode.FORBIDDEN,
                (permission == null || permission.isEmpty()) ? "没有操作权限" : "没有操作权限，需要权限：" + permission);
    }

    /**
     * 无角色权限
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        // checkRoleOr 场景下 e.getRole() 为所需角色列表，作为拒绝原因返回
        String role = e.getRole();
        log.warn("无角色权限，需要角色：{}", role);
        return Result.error(ErrorCode.FORBIDDEN,
                (role == null || role.isEmpty()) ? "没有角色权限" : "没有角色权限，需要以下角色之一：" + role);
    }

    /**
     * 参数校验失败（@RequestBody + @Valid 时抛出）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 参数绑定失败（表单提交 + @Valid）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败：{}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 参数校验失败（方法参数/单参数校验）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数：{}", e.getParameterName());
        return Result.error(ErrorCode.PARAM_ERROR, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getName());
        return Result.error(ErrorCode.PARAM_ERROR, "参数[" + e.getName() + "]类型不正确");
    }

    /**
     * 请求体不可读（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读：{}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR, "请求参数格式错误");
    }

    /**
     * 唯一键冲突（数据库层）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.error("数据唯一键冲突：{}", e.getMessage());
        return Result.error(ErrorCode.DUPLICATE_KEY, "数据已存在，请勿重复提交");
    }

    /**
     * 数据完整性违规（外键约束、非空约束等）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据完整性违规：{}", e.getMessage());
        return Result.error(ErrorCode.BUSINESS_ERROR, "操作失败：数据被其他业务引用或数据不完整");
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超出大小限制：{}", e.getMessage());
        return Result.error(ErrorCode.FILE_SIZE_EXCEEDED, "文件大小超出上传限制");
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR, "请求方法不支持");
    }

    /**
     * 接口不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("接口不存在：{} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.error(ErrorCode.NOT_FOUND, "接口不存在");
    }

    /**
     * 兜底异常：只记录日志，返回通用提示，不回显内部细节
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(ErrorCode.SYSTEM_ERROR, "系统内部错误，请稍后重试");
    }
}
