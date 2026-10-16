package com.gmp.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增角色DTO
 */
@Data
@ApiModel("新增角色请求")
public class RoleSaveDTO {

    /** 角色名称（必填） */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    @ApiModelProperty(value = "角色名称", required = true)
    private String name;

    /** 角色编码（必填且唯一，如 SUPER_ADMIN） */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    @ApiModelProperty(value = "角色编码（唯一）", required = true)
    private String code;

    /** 角色描述 */
    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    @ApiModelProperty("角色描述")
    private String description;

    /** 状态：0-禁用, 1-启用（必填） */
    @NotNull(message = "状态不能为空")
    @ApiModelProperty(value = "状态：0-禁用, 1-启用", required = true)
    private Integer status;
}
