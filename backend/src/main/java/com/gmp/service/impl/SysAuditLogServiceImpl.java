package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.dto.audit.AuditLogExcelVO;
import com.gmp.dto.audit.AuditLogVO;
import com.gmp.entity.SysAuditLog;
import com.gmp.mapper.SysAuditLogMapper;
import com.gmp.service.SysAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志 Service 实现类
 * <p>
 * 负责将存储层的操作类型（INSERT/UPDATE/DELETE/QUERY）与控制器类名，
 * 转换为前端约定的枚举（create/update/...、user/course/...），以及列表字段的重命名映射。
 */
@Slf4j
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements SysAuditLogService {

    /** 操作时间展示格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<AuditLogVO> pageLogs(Integer pageNum, Integer pageSize, String module, String action,
                                     String operator, String startDate, String endDate) {
        Page<SysAuditLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAuditLog> wrapper = buildWrapper(module, action, operator, startDate, endDate);
        wrapper.orderByDesc(SysAuditLog::getOperationTime);

        Page<SysAuditLog> entityPage = baseMapper.selectPage(page, wrapper);
        Page<AuditLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<AuditLogExcelVO> listForExport(String module, String action, String operator,
                                               String startDate, String endDate) {
        LambdaQueryWrapper<SysAuditLog> wrapper = buildWrapper(module, action, operator, startDate, endDate);
        wrapper.orderByDesc(SysAuditLog::getOperationTime);
        return baseMapper.selectList(wrapper).stream().map(this::toExcelVO).collect(Collectors.toList());
    }

    /**
     * 构建查询条件（分页与导出共用）
     */
    private LambdaQueryWrapper<SysAuditLog> buildWrapper(String module, String action, String operator,
                                                         String startDate, String endDate) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        // 模块过滤：前端 code → 控制器类名片段，用 LIKE 匹配
        String modulePattern = moduleCodeToControllerPattern(module);
        if (modulePattern != null) {
            wrapper.like(SysAuditLog::getModule, modulePattern);
        }
        // 操作类型过滤：前端 code → 存储的大写类型
        String operationType = actionCodeToOperationType(action);
        if (operationType != null) {
            wrapper.eq(SysAuditLog::getOperationType, operationType);
        }
        // 操作人过滤：姓名或账号模糊匹配
        if (StrUtil.isNotBlank(operator)) {
            wrapper.and(w -> w.like(SysAuditLog::getRealName, operator)
                    .or().like(SysAuditLog::getUsername, operator));
        }
        // 时间范围过滤
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(SysAuditLog::getOperationTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(SysAuditLog::getOperationTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        return wrapper;
    }

    /**
     * 实体转 VO（字段重命名映射）
     */
    private AuditLogVO toVO(SysAuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setModule(controllerToModuleCode(log.getModule()));
        vo.setAction(operationTypeToAction(log.getOperationType()));
        vo.setDescription(log.getDescription());
        // 操作人优先展示真实姓名，无则退回账号
        vo.setOperator(StrUtil.isNotBlank(log.getRealName()) ? log.getRealName() : log.getUsername());
        vo.setOperatorIp(log.getIp());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setRequestParams(log.getRequestParams());
        // 存储层未记录响应体，固定为空
        vo.setResponseResult(null);
        vo.setStatus(log.getStatus());
        vo.setDuration(log.getExecuteTime());
        vo.setOperateTime(log.getOperationTime() == null ? null
                : log.getOperationTime().format(DATE_TIME_FORMATTER));
        vo.setErrorMsg(log.getErrorMsg());
        return vo;
    }

    /**
     * 实体转导出模型
     */
    private AuditLogExcelVO toExcelVO(SysAuditLog log) {
        AuditLogExcelVO vo = new AuditLogExcelVO();
        vo.setId(log.getId());
        vo.setModule(controllerToModuleCode(log.getModule()));
        vo.setAction(operationTypeToAction(log.getOperationType()));
        vo.setDescription(log.getDescription());
        vo.setOperator(StrUtil.isNotBlank(log.getRealName()) ? log.getRealName() : log.getUsername());
        vo.setOperatorIp(log.getIp());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setStatus(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败");
        vo.setDuration(log.getExecuteTime());
        vo.setOperateTime(log.getOperationTime() == null ? null
                : log.getOperationTime().format(DATE_TIME_FORMATTER));
        return vo;
    }

    /**
     * 控制器类名 → 前端模块 code
     */
    private String controllerToModuleCode(String module) {
        if (StrUtil.isBlank(module)) {
            return "system";
        }
        if (module.contains("User")) {
            return "user";
        }
        if (module.contains("Department")) {
            return "department";
        }
        if (module.contains("Position")) {
            return "position";
        }
        if (module.contains("Video")) {
            return "video";
        }
        if (module.contains("Course")) {
            return "course";
        }
        if (module.contains("Plan")) {
            return "plan";
        }
        if (module.contains("Task")) {
            return "task";
        }
        if (module.contains("Question")) {
            return "question";
        }
        if (module.contains("ExamPaper")) {
            return "paper";
        }
        if (module.contains("Exam")) {
            return "exam";
        }
        return "system";
    }

    /**
     * 前端模块 code → 控制器类名匹配片段（用于 LIKE 过滤）
     */
    private String moduleCodeToControllerPattern(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        switch (code) {
            case "user":
                return "User";
            case "department":
                return "Department";
            case "position":
                return "Position";
            case "course":
                return "Course";
            case "video":
                return "Video";
            case "plan":
                return "Plan";
            case "task":
                return "Task";
            case "question":
                return "Question";
            case "paper":
                return "ExamPaper";
            case "exam":
                return "Exam";
            case "system":
                return "Auth";
            default:
                return null;
        }
    }

    /**
     * 操作类型（存储大写）→ 前端 action code
     */
    private String operationTypeToAction(String operationType) {
        if (StrUtil.isBlank(operationType)) {
            return "query";
        }
        switch (operationType.toUpperCase()) {
            case "INSERT":
                return "create";
            case "UPDATE":
                return "update";
            case "DELETE":
                return "delete";
            case "EXPORT":
                return "export";
            case "IMPORT":
                return "import";
            case "APPROVE":
                return "approve";
            case "LOGIN":
                return "login";
            default:
                return "query";
        }
    }

    /**
     * 前端 action code → 存储的操作类型（大写，空表示不过滤）
     */
    private String actionCodeToOperationType(String action) {
        if (StrUtil.isBlank(action)) {
            return null;
        }
        switch (action) {
            case "create":
                return "INSERT";
            case "update":
                return "UPDATE";
            case "delete":
                return "DELETE";
            case "query":
                return "QUERY";
            case "export":
                return "EXPORT";
            case "import":
                return "IMPORT";
            case "approve":
                return "APPROVE";
            case "login":
                return "LOGIN";
            default:
                return null;
        }
    }
}
