package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改岗位请求DTO
 */
@Data
@ApiModel("修改岗位请求")
public class PositionUpdateDTO {

    @NotNull(message = "岗位ID不能为空")
    @ApiModelProperty("岗位ID")
    private Long id;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称长度不能超过100")
    @ApiModelProperty("岗位名称")
    private String name;

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 50, message = "岗位编码长度不能超过50")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "岗位编码只能包含字母、数字、下划线和横线")
    @ApiModelProperty("岗位编码")
    private String code;

    @Size(max = 500, message = "岗位描述长度不能超过500")
    @ApiModelProperty("岗位描述")
    private String description;

    @NotNull(message = "所属部门不能为空")
    @ApiModelProperty("所属部门ID")
    private Long departmentId;

    @Size(max = 20, message = "岗位级别长度不能超过20")
    @ApiModelProperty("岗位级别：初级/中级/高级/专家")
    private String level;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;
}
