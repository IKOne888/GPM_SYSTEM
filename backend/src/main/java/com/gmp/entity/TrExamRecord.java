package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试记录实体类
 */
@Data
@TableName("tr_exam_record")
public class TrExamRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 试卷ID */
    private Long paperId;

    /** 用户ID */
    private Long userId;

    /** 第几次考试 */
    private Integer retryCount;

    /** 得分 */
    private Integer score;

    /** 是否通过：0-未通过 1-已通过 */
    private Integer pass;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 答题时长（秒） */
    private Integer duration;

    /** 状态：0-考试中 1-已提交 2-已批阅 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
