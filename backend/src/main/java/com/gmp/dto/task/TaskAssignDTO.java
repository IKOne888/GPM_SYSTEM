package com.gmp.dto.task;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 培训任务分配 DTO
 * <p>
 * assigneeIds 支持一次为多名执行人分配同一任务，Service 会按执行人拆分为多条任务记录。
 */
@Data
@ApiModel("培训任务分配参数")
public class TaskAssignDTO {

    @ApiModelProperty("任务名称")
    @NotBlank(message = "任务名称不能为空")
    private String name;

    @ApiModelProperty("关联培训计划ID（可选）")
    private Long planId;

    @ApiModelProperty("培训课程ID")
    @NotNull(message = "请选择培训课程")
    private Long courseId;

    @ApiModelProperty("执行人ID列表")
    @NotEmpty(message = "请选择执行人")
    private List<Long> assigneeIds;

    @ApiModelProperty("截止日期")
    @NotNull(message = "请选择截止日期")
    private LocalDate deadline;

    @ApiModelProperty("任务说明")
    private String description;
}
