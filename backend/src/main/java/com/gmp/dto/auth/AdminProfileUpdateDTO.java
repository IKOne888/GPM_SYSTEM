package com.gmp.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 管理端「个人中心」资料修改 DTO
 * <p>
 * 仅允许修改非敏感基础资料；用户名、部门、岗位、角色由管理员统一维护，个人不可自改。
 */
@Data
@ApiModel("个人资料修改参数")
public class AdminProfileUpdateDTO {

    /** 真实姓名（≤50） */
    @ApiModelProperty(value = "真实姓名", example = "张三")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    /** 手机号（可为空串，非空时须符合 11 位手机号格式） */
    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（须符合邮箱格式） */
    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}