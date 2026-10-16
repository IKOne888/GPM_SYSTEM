package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频学习记录实体类（对应表 tr_video_study_record）
 * <p>
 * 用于记录「某个学员」在「某个培训任务」下对「某个视频课件」的学习情况，
 * 支持断点续播（last_position）与进度统计（video_progress）。
 * <p>
 * 说明：该表无 deleted 逻辑删除列（与 tr_training_task、tr_exam_record 一致），
 * 因此实体上不使用 @TableLogic。
 */
@Data
@TableName("tr_video_study_record")
public class TrVideoStudyRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 培训任务ID */
    private Long taskId;

    /** 视频课件ID */
    private Long videoId;

    /** 学员用户ID */
    private Long userId;

    /** 累计学习时长（秒） */
    private Integer studyDuration;

    /** 视频播放进度（百分比，0~100，保留两位小数） */
    private BigDecimal videoProgress;

    /** 上次播放位置（秒），用于断点续播 */
    private Integer lastPosition;

    /** 是否已完成：0-未完成 1-已完成（单视频进度≥95% 视为完成） */
    private Integer isCompleted;

    /** 最后学习时间 */
    private LocalDateTime lastStudyTime;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
