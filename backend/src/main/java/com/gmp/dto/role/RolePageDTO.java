package com.gmp.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 角色分页查询DTO
 */
@Data
@ApiModel("角色分页查询请求")
public class RolePageDTO {

    /** 页码（默认1） */
    @Min(value = 1, message = "页码最小为1")
    @ApiModelProperty(value = "页码，默认1")
    private Integer pageNum = 1;

    /** 每页条数（默认10，最大100） */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @ApiModelProperty(value = "每页条数，默认10")
    private Integer pageSize = 10;

    /** 角色名称（模糊查询） */
    @ApiModelProperty("角色名称（模糊查询）")
    private String name;

    /** 角色编码（模糊查询） */
    @ApiModelProperty("角色编码（模糊查询）")
    private String code;

    /** 状态：0-禁用, 1-启用（精确查询） */
    @ApiModelProperty("状态：0-禁用, 1-启用")
    private Integer status;
}
