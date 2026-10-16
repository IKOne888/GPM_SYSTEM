package com.gmp.dto.demand;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 培训需求处理 DTO
 * <p>
 * 用于「采纳 / 拒绝」需求场景：status 决定处理结果，opinion 记录处理意见。
 */
@Data
@ApiModel("培训需求处理参数")
public class DemandHandleDTO {

    /** 处理结果：1-已采纳 2-已拒绝 */
    @ApiModelProperty(value = "处理结果：1-已采纳 2-已拒绝", required = true)
    @NotNull(message = "请选择处理结果")
    private Integer status;

    /** 处理意见（拒绝时必填，采纳时选填） */
    @ApiModelProperty("处理意见（拒绝时必填）")
    private String opinion;
}
