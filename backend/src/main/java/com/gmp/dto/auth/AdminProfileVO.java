package com.gmp.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端「个人中心」资料展示 VO
 * <p>
 * 在用户基础字段之上，补充部门名称、岗位名称与角色编码列表，
 * 便于前端直接展示而无需二次请求；不含密码等敏感字段。
 */
@Data
@ApiModel("个人中心资料")
public class AdminProfileVO {

    /** 用户ID */
    @ApiModelProperty("用户ID")
    private Long id;

    /** 用户名（登录账号，不可修改） */
    @ApiModelProperty("用户名")
    private String username;

    /** 真实姓名 */
    @ApiModelProperty("真实姓名")
    private String realName;

    /** 手机号 */
    @ApiModelProperty("手机号")
    private String phone;

    /** 邮箱 */
    @ApiModelProperty("邮箱")
    private String email;

    /** 部门ID */
    @ApiModelProperty("部门ID")
    private Long departmentId;

    /** 部门名称（Service 层回填） */
    @ApiModelProperty("部门名称")
    private String departmentName;

    /** 岗位ID */
    @ApiModelProperty("岗位ID")
    private Long positionId;

    /** 岗位名称（Service 层回填） */
    @ApiModelProperty("岗位名称")
    private String positionName;

    /** 角色编码列表（如 SUPER_ADMIN），前端用于权限控制与标签展示 */
    @ApiModelProperty("角色编码列表")
    private List<String> roles;

    /** 最后登录时间 */
    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    /** 状态：0-禁用 1-启用 */
    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;
}