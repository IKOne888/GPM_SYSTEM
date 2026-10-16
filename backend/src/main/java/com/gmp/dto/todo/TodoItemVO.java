package com.gmp.dto.todo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办明细条目 VO
 */
@Data
@ApiModel("待办明细条目")
public class TodoItemVO {

    /** 业务ID（任务ID / 需求ID / 计划ID） */
    @ApiModelProperty("业务ID")
    private Long id;

    /** 标题（任务名 / 需求标题 / 计划名） */
    @ApiModelProperty("标题")
    private String title;

    /** 补充说明（如「部门 · 学员」「提交人 xxx」） */
    @ApiModelProperty("补充说明")
    private String extra;

    /** 时间（任务创建时间 / 需求登记时间 / 计划提交时间） */
    @ApiModelProperty("时间")
    private LocalDateTime time;

    /** 审核阶段（仅待审核任务使用：leader-待主管审核 qa-待QA复核） */
    @ApiModelProperty("审核阶段：leader/qa")
    private String stage;
}