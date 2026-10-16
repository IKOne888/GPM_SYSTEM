package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类
 */
@Data
@TableName("tr_course")
public class TrCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Long categoryId;

    /** 所属部门ID（课程归属部门，用于按部门维度统计与筛选） */
    private Long departmentId;

    /** 适用岗位ID（课程面向的岗位，可为空表示全岗位通用） */
    private Long positionId;

    private String sopCode;

    private String sopVersion;

    private String trainingObjective;

    private BigDecimal trainingHours;

    private Integer passScore;

    private Integer retrainPeriod;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    private Integer deleted;
}
