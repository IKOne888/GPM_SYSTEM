package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训需求实体类（对应表 tr_training_demand）
 * <p>
 * 用于登记各部门提出的培训需求，形成可追溯的需求台账。
 * 状态字段 status 语义：0-待处理 1-已采纳 2-已拒绝。
 * 说明：需求处理意见（采纳备注 / 拒绝原因）复用 reason 字段存储，避免为单一场景新增数据库列。
 */
@Data
@TableName("tr_training_demand")
public class TrTrainingDemand {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 需求标题 */
    private String title;

    /** 需求内容（详细描述） */
    private String content;

    /** 培训主题 */
    private String trainingTopic;

    /** 培训对象（预期参训人群描述） */
    private String trainingTarget;

    /** 需求原因 / 处理意见（拒绝时必填拒绝原因，采纳时记录采纳备注） */
    private String reason;

    /** 申请部门ID（关联 sys_department.id） */
    private Long departmentId;

    /** 提交人用户ID（学员端提交时写入，用于「我的申报」隔离与结果通知；NULL-管理端代录） */
    private Long submitUserId;

    /** 状态：0-待处理 1-已采纳 2-已拒绝 */
    private Integer status;

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
