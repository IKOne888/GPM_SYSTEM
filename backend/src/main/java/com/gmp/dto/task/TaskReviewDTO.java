package com.gmp.dto.task;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 培训任务审核 DTO
 * <p>
 * 用于「主管审核」与「QA 复核」两个接口的入参，
 * pass=true 表示通过，pass=false 表示退回（退回时 remark 必填，由 Service 层校验）。
 */
@Data
@ApiModel("培训任务审核参数")
public class TaskReviewDTO {

    /** 审核结论：true-通过 false-退回 */
    @ApiModelProperty(value = "审核结论：true-通过 false-退回", required = true)
    @NotNull(message = "审核结论不能为空")
    private Boolean pass;

    /** 审核意见：通过时可空；退回时必填，用于告知学员退回原因 */
    @ApiModelProperty("审核意见（退回时必填）")
    @Size(max = 255, message = "审核意见长度不能超过255")
    private String remark;
}