package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 个人资料修改 DTO
 * <p>
 * 仅允许修改非敏感的基础资料；用户名、角色、部门等由管理员维护，学员端不可改。
 */
@Data
@ApiModel("个人资料修改参数")
public class ProfileUpdateDTO {

    @ApiModelProperty(value = "真实姓名", example = "张三")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @ApiModelProperty(value = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
