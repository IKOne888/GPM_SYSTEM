package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训任务实体类
 */
@Data
@TableName("tr_training_task")
public class TrTrainingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String name;

    private Long planId;

    private Long courseId;

    private Long userId;

    private Integer taskType;

    /** 任务来源：1-系统分配（计划/管理员） 2-自主报名（课程中心） */
    private Integer source;

    /** 任务说明 */
    private String description;

    private Integer status;

    /**
     * 审核状态（两级审核闭环）：
     * null-未进入审核链 0-待主管审核 1-待QA复核 2-已复核通过 3-已退回
     */
    private Integer reviewStatus;

    /** 一级审核人（部门主管）用户ID */
    private Long leaderReviewUserId;

    /** 一级审核时间 */
    private LocalDateTime leaderReviewTime;

    /** 一级审核意见（主管审核说明） */
    private String leaderReviewRemark;

    /** 二级复核人（QA）用户ID */
    private Long qaReviewUserId;

    /** 二级复核时间 */
    private LocalDateTime qaReviewTime;

    /** 二级复核意见（QA 复核说明 / 免考确认理由） */
    private String qaReviewRemark;

    private LocalDateTime deadline;

    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    private Integer studyDuration;

    private BigDecimal videoProgress;

    private Integer examScore;

    private Integer examPass;

    private Integer retryCount;

    private Integer maxRetry;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
