package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 修改密码 DTO
 * <p>
 * 安全要求：必须校验旧密码；新密码强度为 6~20 位且同时包含字母和数字。
 */
@Data
@ApiModel("修改密码参数")
public class PasswordUpdateDTO {

    @ApiModelProperty(value = "旧密码", required = true)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @ApiModelProperty(value = "新密码（6-20位，需同时包含字母和数字）", required = true)
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\S]{6,20}$",
            message = "新密码需为6-20位且同时包含字母和数字")
    private String newPassword;
}
