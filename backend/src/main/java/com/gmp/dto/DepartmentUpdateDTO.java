package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改部门请求DTO
 */
@Data
@ApiModel("修改部门请求")
public class DepartmentUpdateDTO {

    @NotNull(message = "部门ID不能为空")
    @ApiModelProperty("部门ID")
    private Long id;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100")
    @ApiModelProperty("部门名称")
    private String name;

    @NotBlank(message = "部门编码不能为空")
    @Size(max = 50, message = "部门编码长度不能超过50")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "部门编码只能包含字母、数字、下划线和横线")
    @ApiModelProperty("部门编码")
    private String code;

    @ApiModelProperty("父部门ID")
    private Long parentId;

    @Size(max = 50, message = "负责人长度不能超过50")
    @ApiModelProperty("负责人")
    private String leader;

    @Size(max = 20, message = "联系电话长度不能超过20")
    @ApiModelProperty("联系电话")
    private String phone;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;
}
