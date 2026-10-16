package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量删除用户请求DTO
 */
@Data
@ApiModel("批量删除用户请求")
public class BatchDeleteDTO {

    @NotEmpty(message = "请选择要删除的用户")
    @Size(max = 100, message = "单次批量删除用户数不能超过100")
    @ApiModelProperty("用户ID列表")
    private List<Long> ids;
}
