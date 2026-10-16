package com.gmp.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色绑定菜单DTO（全量替换式绑定）
 */
@Data
@ApiModel("角色绑定菜单请求")
public class RoleBindMenuDTO {

    /** 角色ID（必填） */
    @NotNull(message = "角色ID不能为空")
    @ApiModelProperty(value = "角色ID", required = true)
    private Long roleId;

    /** 菜单ID列表（全量替换，传空列表表示清除所有菜单） */
    @ApiModelProperty("菜单ID列表")
    private List<Long> menuIds;
}
