package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 新增课程请求DTO
 */
@Data
@ApiModel("新增课程请求")
public class CourseAddDTO {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 200, message = "课程名称长度不能超过200")
    @ApiModelProperty("课程名称")
    private String name;

    @NotBlank(message = "课程编码不能为空")
    @Size(max = 50, message = "课程编码长度不能超过50")
    @ApiModelProperty("课程编码")
    private String code;

    @NotNull(message = "课程分类不能为空")
    @ApiModelProperty("课程分类ID")
    private Long categoryId;

    @ApiModelProperty("所属部门ID（可为空）")
    private Long departmentId;

    @ApiModelProperty("适用岗位ID（可为空，表示全岗位通用）")
    private Long positionId;

    @Size(max = 50, message = "SOP编号长度不能超过50")
    @ApiModelProperty("SOP编号")
    private String sopCode;

    @Size(max = 20, message = "SOP版本号长度不能超过20")
    @ApiModelProperty("SOP版本号")
    private String sopVersion;

    @Size(max = 1000, message = "培训目标长度不能超过1000")
    @ApiModelProperty("培训目标")
    private String trainingObjective;

    @NotNull(message = "培训学时不能为空")
    @DecimalMin(value = "0.1", message = "培训学时最小为0.1")
    @DecimalMax(value = "999.99", message = "培训学时最大为999.99")
    @ApiModelProperty("培训学时")
    private BigDecimal trainingHours;

    @Min(value = 0, message = "及格分数最小为0")
    @Max(value = 100, message = "及格分数最大为100")
    @ApiModelProperty("及格分数")
    private Integer passScore;

    @Min(value = 1, message = "复训周期最小为1天")
    @Max(value = 3650, message = "复训周期最大为3650天")
    @ApiModelProperty("复训周期（天）")
    private Integer retrainPeriod;

    @ApiModelProperty("状态：0-停用 1-启用")
    private Integer status;
}
