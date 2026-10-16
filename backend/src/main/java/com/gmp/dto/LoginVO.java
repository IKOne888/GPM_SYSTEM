package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录成功响应VO
 */
@Data
@ApiModel("登录成功响应")
public class LoginVO {

    @ApiModelProperty("访问令牌")
    private String token;

    @ApiModelProperty("用户信息（不含敏感字段）")
    private UserInfoVO user;

    public LoginVO(String token, UserInfoVO user) {
        this.token = token;
        this.user = user;
    }
}
