package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 培训计划-课程行 返回 VO
 */
@Data
@ApiModel("培训计划课程行")
public class PlanCourseVO {

    @ApiModelProperty("关联记录ID")
    private Long id;

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("讲师")
    private String trainer;

    @ApiModelProperty("计划培训日期")
    private LocalDate plannedDate;
}
