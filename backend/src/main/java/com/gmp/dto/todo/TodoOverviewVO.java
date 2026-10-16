package com.gmp.dto.todo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 统一待办中心聚合 VO
 * <p>
 * 聚合三类待办：待审核任务、待处理需求、待审批计划；数量按当前登录用户角色自适应。
 */
@Data
@ApiModel("统一待办中心")
public class TodoOverviewVO {

    /** 待审核任务 */
    @ApiModelProperty("待审核任务")
    private TodoGroupVO reviewPending;

    /** 待处理需求 */
    @ApiModelProperty("待处理需求")
    private TodoGroupVO demandPending;

    /** 待审批计划 */
    @ApiModelProperty("待审批计划")
    private TodoGroupVO planPending;

    /** 待办总数（三类之和） */
    @ApiModelProperty("待办总数")
    private Long total;
}