package com.gmp.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 修改角色DTO
 * <p>注意：角色编码(code)不允许修改，作为唯一键保留不变。</p>
 */
@Data
@ApiModel("修改角色请求")
public class RoleUpdateDTO {

    /** 角色ID（必填） */
    @NotNull(message = "角色ID不能为空")
    @ApiModelProperty(value = "角色ID", required = true)
    private Long id;

    /** 角色名称 */
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    @ApiModelProperty("角色名称")
    private String name;

    /** 角色描述 */
    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    @ApiModelProperty("角色描述")
    private String description;

    /** 状态：0-禁用, 1-启用 */
    @ApiModelProperty("状态：0-禁用, 1-启用")
    private Integer status;
}
