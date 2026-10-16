package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员任务列表 VO
 * <p>
 * statusCode 为真实数据库状态（0~4），status 为前端可直接使用的字符串枚举；
 * 当任务未完成且已过截止时间时，statusCode 动态呈现为 4（已逾期）。
 */
@Data
@ApiModel("学员任务信息")
public class StudyTaskVO {

    @ApiModelProperty("任务ID")
    private Long id;

    @ApiModelProperty("任务名称")
    private String name;

    @ApiModelProperty("所属计划名称")
    private String planName;

    @ApiModelProperty("培训课程ID")
    private Long courseId;

    @ApiModelProperty("培训课程名称")
    private String courseName;

    @ApiModelProperty("截止时间")
    private LocalDateTime deadline;

    @ApiModelProperty("状态码：0-待学习 1-学习中 2-待考试 3-已完成 4-已逾期")
    private Integer statusCode;

    @ApiModelProperty("状态：not_started/studying/waiting_exam/completed/overdue/pending_leader_review/pending_qa_review/rejected")
    private String status;

    @ApiModelProperty("审核状态：0-待主管审核 1-待QA复核 2-已复核通过 3-已退回（null 表示未进入审核链）")
    private Integer reviewStatus;

    @ApiModelProperty("审核状态文案（待主管审核/待QA复核/已复核通过/已退回）")
    private String reviewStatusText;

    @ApiModelProperty("退回意见（被退回时展示，便于学员了解原因）")
    private String rejectRemark;

    @ApiModelProperty("视频学习进度（百分比）")
    private BigDecimal videoProgress;

    @ApiModelProperty("学习时长（秒）")
    private Integer studyDuration;

    @ApiModelProperty("考试成绩")
    private Integer examScore;

    @ApiModelProperty("是否通过考试：0-未通过 1-已通过")
    private Integer examPass;

    @ApiModelProperty("已考试次数")
    private Integer retryCount;

    @ApiModelProperty("最大考试次数")
    private Integer maxRetry;

    @ApiModelProperty("是否可以参加考试")
    private Boolean canExam;

    @ApiModelProperty("任务说明")
    private String description;

    @ApiModelProperty("任务创建时间")
    private LocalDateTime createTime;
}
