package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 学员任务统计 VO
 * <p>
 * 用于首页工作台顶部的统计卡片：待学习 / 学习中 / 待考试 / 已完成 / 已逾期。
 * 说明：「已逾期」为动态计算结果（未完成且已过截止时间），非数据库持久化状态。
 */
@Data
@ApiModel("任务统计")
public class StudyStatsVO {

    @ApiModelProperty("任务总数")
    private Long total;

    @ApiModelProperty("待学习数量")
    private Long notStarted;

    @ApiModelProperty("学习中数量")
    private Long studying;

    @ApiModelProperty("待考试数量")
    private Long waitingExam;

    @ApiModelProperty("已完成数量")
    private Long completed;

    @ApiModelProperty("已逾期数量")
    private Long overdue;

    @ApiModelProperty("待审核数量（已达标并进入两级审核链的任务数）")
    private Long pendingReview;

    @ApiModelProperty("已退回数量（审核未通过，需重新学习考试）")
    private Long rejected;
}
