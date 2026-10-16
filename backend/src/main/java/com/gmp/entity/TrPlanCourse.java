package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训计划-课程关联实体类（对应表 tr_plan_course）
 * <p>
 * 一个培训计划可关联多门课程，并记录每门课程的讲师与计划培训日期。
 * 关联关系以「计划ID + 课程ID」唯一。
 */
@Data
@TableName("tr_plan_course")
public class TrPlanCourse {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划ID */
    private Long planId;

    /** 课程ID */
    private Long courseId;

    /** 讲师 */
    private String trainer;

    /** 计划培训日期 */
    private LocalDate plannedDate;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
