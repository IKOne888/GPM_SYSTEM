package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 培训计划-课程行 DTO
 * <p>
 * 对应计划编辑弹窗中的「培训课程」表格每一行，包含课程、讲师与计划培训日期。
 */
@Data
@ApiModel("培训计划课程行")
public class PlanCourseDTO {

    @ApiModelProperty("课程ID")
    @NotNull(message = "课程不能为空")
    private Long courseId;

    @ApiModelProperty("讲师")
    private String trainer;

    @ApiModelProperty("计划培训日期")
    private LocalDate plannedDate;
}
