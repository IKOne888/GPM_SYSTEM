package com.gmp.dto.task;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 培训任务列表返回 VO
 * <p>
 * status 由数字枚举转换为前端字符串（not_started/in_progress/completed/overdue）；
 * progress 为学习进度百分比，取自 video_progress。
 */
@Data
@ApiModel("培训任务信息")
public class TaskVO {

    @ApiModelProperty("任务ID")
    private Long id;

    @ApiModelProperty("任务名称")
    private String name;

    @ApiModelProperty("所属计划名称")
    private String planName;

    @ApiModelProperty("培训课程名称")
    private String courseName;

    @ApiModelProperty("执行人姓名")
    private String assigneeName;

    @ApiModelProperty("执行人所属部门名称")
    private String departmentName;

    @ApiModelProperty("截止日期")
    private LocalDate deadline;

    @ApiModelProperty("状态：not_started/in_progress/completed/overdue")
    private String status;

    @ApiModelProperty("审核状态：0-待主管审核 1-待QA复核 2-已复核通过 3-已退回（null 表示未进入审核链）")
    private Integer reviewStatus;

    @ApiModelProperty("审核状态文案（待主管审核/待QA复核/已复核通过/已退回）")
    private String reviewStatusText;

    @ApiModelProperty("一级审核意见（主管）")
    private String leaderReviewRemark;

    @ApiModelProperty("二级审核意见（QA）")
    private String qaReviewRemark;

    @ApiModelProperty("学习进度（百分比）")
    private Integer progress;

    @ApiModelProperty("任务说明")
    private String description;
}
