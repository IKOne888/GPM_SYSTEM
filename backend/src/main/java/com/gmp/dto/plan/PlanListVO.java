package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 培训计划下拉列表 VO
 * <p>
 * 供任务分配、其它表单关联计划时选择使用，仅返回必要字段。
 */
@Data
@ApiModel("培训计划下拉项")
public class PlanListVO {

    @ApiModelProperty("计划ID")
    private Long id;

    @ApiModelProperty("计划名称")
    private String name;
}
