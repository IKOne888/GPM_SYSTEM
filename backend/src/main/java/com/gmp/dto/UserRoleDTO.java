package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 用户角色分配请求DTO
 */
@Data
@ApiModel("用户角色分配请求")
public class UserRoleDTO {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty("用户ID")
    private Long userId;

    @Size(min = 1, message = "至少分配一个角色")
    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;
}
