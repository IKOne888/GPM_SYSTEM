package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 新增用户请求DTO
 */
@Data
@ApiModel("新增用户请求")
public class UserAddDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    @ApiModelProperty("用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6-20位")
    @ApiModelProperty("密码（明文，后端加密存储）")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50")
    @ApiModelProperty("真实姓名")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    @ApiModelProperty("邮箱")
    private String email;

    @NotNull(message = "部门不能为空")
    @ApiModelProperty("部门ID")
    private Long departmentId;

    @NotNull(message = "岗位不能为空")
    @ApiModelProperty("岗位ID")
    private Long positionId;

    @ApiModelProperty("是否部门负责人：0-否 1-是")
    private Integer isDepartmentLeader;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;

    // 角色列表可选：不传时默认赋予"普通员工"角色
    @ApiModelProperty("角色ID列表（不传则默认普通员工）")
    private List<Long> roleIds;
}
