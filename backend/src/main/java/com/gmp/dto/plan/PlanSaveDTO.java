package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 培训计划保存 DTO（新增 / 编辑共用）
 * <p>
 * 前端 year 以字符串 "YYYY" 提交，Jackson 会自动转换为 Integer；
 * startDate/endDate/plannedDate 以 "YYYY-MM-DD" 提交，转换为 LocalDate。
 */
@Data
@ApiModel("培训计划保存参数")
public class PlanSaveDTO {

    @ApiModelProperty("计划ID（编辑时必填，新增时为空）")
    private Long id;

    @ApiModelProperty("计划编号")
    @NotBlank(message = "计划编号不能为空")
    private String code;

    @ApiModelProperty("计划名称")
    @NotBlank(message = "计划名称不能为空")
    private String name;

    @ApiModelProperty("计划年度")
    @NotNull(message = "计划年度不能为空")
    private Integer year;

    @ApiModelProperty("计划类型：1-年度计划 2-专项计划（不传默认 1）")
    private Integer planType;

    @ApiModelProperty("计划描述")
    private String description;

    @ApiModelProperty("培训对象部门ID列表")
    private List<Long> targetDepartments;

    @ApiModelProperty("开始日期")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @ApiModelProperty("计划关联的课程列表")
    @Valid
    private List<PlanCourseDTO> courses;
}
