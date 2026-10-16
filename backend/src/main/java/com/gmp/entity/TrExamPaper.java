package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷实体类（对应表 tr_exam_paper）
 * <p>
 * 状态 status 取值：0-草稿 1-已发布 2-已归档。
 * 组卷方式 compositionType 取值：manual-手动选题 random-随机组卷；
 * 随机组卷规则以 JSON 形式存放于 rules 字段。
 */
@Data
@TableName("tr_exam_paper")
public class TrExamPaper {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷编号（唯一，如 PAPER-2026-001） */
    private String code;

    /** 试卷名称 */
    private String name;

    /** 课程ID（可为空，试卷可不绑定具体课程） */
    private Long courseId;

    /** 总分 */
    private Integer totalScore;

    /** 及格分数 */
    private Integer passScore;

    /** 考试时长（分钟） */
    private Integer examDuration;

    /** 最大补考次数 */
    private Integer maxRetry;

    /** 题目数量 */
    private Integer questionCount;

    /** 组卷方式：manual-手动选题 random-随机组卷 */
    private String compositionType;

    /** 组卷规则（JSON 字符串） */
    private String rules;

    /** 状态：0-草稿 1-已发布 2-已归档 */
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新人（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 逻辑删除标记：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
