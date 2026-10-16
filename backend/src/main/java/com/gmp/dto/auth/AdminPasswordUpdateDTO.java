package com.gmp.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 管理端「个人中心」修改密码 DTO
 * <p>
 * 安全要求：必须校验旧密码；新密码强度为 6~20 位且同时包含字母和数字；
 * 新密码与确认密码必须一致（一致性在 Service 层校验，便于返回统一业务错误码）。
 */
@Data
@ApiModel("修改密码参数")
public class AdminPasswordUpdateDTO {

    /** 原密码（必填） */
    @ApiModelProperty(value = "原密码", required = true)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码（6-20位，需同时包含字母和数字） */
    @ApiModelProperty(value = "新密码（6-20位，需同时包含字母和数字）", required = true)
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\S]{6,20}$",
            message = "新密码需为6-20位且同时包含字母和数字")
    private String newPassword;

    /** 确认新密码（必须与新密码一致） */
    @ApiModelProperty(value = "确认新密码", required = true)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}