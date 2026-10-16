package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表返回VO（脱敏，含角色信息）
 */
@Data
@ApiModel("用户列表信息")
public class UserListVO {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("部门ID")
    private Long departmentId;

    @ApiModelProperty("部门名称")
    private String departmentName;

    @ApiModelProperty("岗位ID")
    private Long positionId;

    @ApiModelProperty("岗位名称")
    private String positionName;

    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;

    @ApiModelProperty("角色编码列表")
    private List<String> roleCodes;

    @ApiModelProperty("角色名称列表")
    private List<String> roleNames;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;
}
