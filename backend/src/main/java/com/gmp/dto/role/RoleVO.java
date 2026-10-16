package com.gmp.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色VO（列表展示）
 */
@Data
@ApiModel("角色列表展示")
public class RoleVO {

    /** 角色ID */
    @ApiModelProperty("角色ID")
    private Long id;

    /** 角色名称 */
    @ApiModelProperty("角色名称")
    private String name;

    /** 角色编码 */
    @ApiModelProperty("角色编码")
    private String code;

    /** 角色描述 */
    @ApiModelProperty("角色描述")
    private String description;

    /** 状态：0-禁用, 1-启用 */
    @ApiModelProperty("状态：0-禁用, 1-启用")
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    /** 关联用户数（从 sys_user_role 聚合计数） */
    @ApiModelProperty("关联用户数")
    private Integer userCount;
}
