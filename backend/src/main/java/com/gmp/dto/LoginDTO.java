package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录请求DTO
 */
@Data
@ApiModel("用户登录请求")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    @ApiModelProperty("用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码长度不能超过50")
    @ApiModelProperty("密码")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 36, message = "验证码key长度不正确")
    @ApiModelProperty("验证码key（生成验证码接口返回）")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 10, message = "验证码长度不正确")
    @ApiModelProperty("验证码内容")
    private String captchaCode;
}
