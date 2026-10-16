package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 修改用户状态请求DTO
 */
@Data
@ApiModel("修改用户状态请求")
public class UserStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值仅支持0或1")
    @Max(value = 1, message = "状态取值仅支持0或1")
    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;
}
