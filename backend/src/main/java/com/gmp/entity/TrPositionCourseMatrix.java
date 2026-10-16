package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 岗位培训矩阵实体类
 * <p>
 * 描述"岗位-课程"的培训要求：是否必修、培训频次、复训周期等。
 */
@Data
@TableName("tr_position_course_matrix")
public class TrPositionCourseMatrix {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 岗位ID */
    private Long positionId;

    /** 课程ID */
    private Long courseId;

    /** 是否必修：0-选修 1-必修 */
    private Integer isRequired;

    /** 培训频次：onboard-入职时 yearly-每年 half_year-每半年 quarterly-每季度 once-一次性 */
    private String frequency;

    /** 复训周期（天） */
    private Integer retrainPeriod;

    /** 及格分数 */
    private Integer passScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
