package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 计划审批参数 DTO
 * <p>
 * 审批通过 / 驳回共用，comment 为审批意见（驳回时必填）。
 */
@Data
@ApiModel("计划审批参数")
public class ApprovalDTO {

    @ApiModelProperty("审批意见（驳回原因）")
    private String comment;
}
