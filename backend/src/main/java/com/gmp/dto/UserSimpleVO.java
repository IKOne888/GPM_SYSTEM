package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户下拉选项 VO（脱敏，仅暴露必要字段）
 * <p>
 * 用于任务分配、执行人选择等场景，绝不包含密码等敏感信息。
 */
@Data
@ApiModel("用户下拉选项")
public class UserSimpleVO {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("用户名")
    private String username;
}
