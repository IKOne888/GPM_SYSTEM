package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训计划实体类（对应表 tr_training_plan）
 * <p>
 * 状态字段 status 语义：0-草稿 1-待审批 2-已通过 3-已驳回 4-已发布。
 * 前端展示的审批状态（draft/pending/approved/rejected）由 Service 层完成与数字枚举的互转。
 */
@Data
@TableName("tr_training_plan")
public class TrTrainingPlan {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划编号（唯一，如 PLAN-2026-001） */
    private String code;

    /** 计划名称 */
    private String name;

    /** 计划年度 */
    private Integer year;

    /** 计划类型：1-年度计划 2-专项计划 */
    private Integer planType;

    /** 计划描述 */
    private String description;

    /** 培训对象部门ID列表（JSON 文本，如 [1,2]） */
    private String targetDepartments;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 状态：0-草稿 1-待审批 2-已通过 3-已驳回 4-已发布 */
    private Integer status;

    /** 提交审批时间 */
    private LocalDateTime submitTime;

    /** 提交审批人用户ID（用于「自提自批」校验：审批人不得等于提交人） */
    private Long submitUserId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批人ID */
    private Long approveUserId;

    /** 审批意见（驳回原因） */
    private String approveRemark;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人（插入时自动填充为当前登录账号 username） */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新人（插入/更新时自动填充为当前登录账号 username） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 逻辑删除标记：0-未删除 1-已删除（MyBatis-Plus 自动过滤） */
    @TableLogic
    private Integer deleted;
}
