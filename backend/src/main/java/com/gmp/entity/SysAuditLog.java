package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计追踪实体类
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String operationType;

    private String module;

    private String description;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String beforeData;

    private String afterData;

    private String ip;

    private String userAgent;

    private Integer executeTime;

    private Integer status;

    private String errorMsg;

    private LocalDateTime operationTime;
}
