package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员任务详情 VO
 * <p>
 * 聚合「任务 + 课程信息 + 视频课件（含本人进度）+ 课程资料 + 考试信息」，
 * 供任务详情页一次性渲染。
 */
@Data
@ApiModel("任务详情")
public class TaskDetailVO {

    @ApiModelProperty("任务ID")
    private Long id;

    @ApiModelProperty("任务名称")
    private String name;

    @ApiModelProperty("任务说明")
    private String description;

    @ApiModelProperty("所属计划名称")
    private String planName;

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

    @ApiModelProperty("退回意见（被退回时展示）")
    private String rejectRemark;

    @ApiModelProperty("视频学习进度（百分比）")
    private BigDecimal videoProgress;

    @ApiModelProperty("学习时长（秒）")
    private Integer studyDuration;

    @ApiModelProperty("考试成绩")
    private Integer examScore;

    @ApiModelProperty("是否通过考试：0-未通过 1-已通过")
    private Integer examPass;

    @ApiModelProperty("任务创建时间")
    private LocalDateTime createTime;

    // ===================== 课程信息 =====================

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("课程编码")
    private String courseCode;

    @ApiModelProperty("SOP编号")
    private String sopCode;

    @ApiModelProperty("SOP版本")
    private String sopVersion;

    @ApiModelProperty("培训目标")
    private String trainingObjective;

    @ApiModelProperty("培训学时")
    private BigDecimal trainingHours;

    @ApiModelProperty("课程及格分")
    private Integer passScore;

    @ApiModelProperty("再培训周期（天），用于计算证书有效期")
    private Integer retrainPeriod;

    // ===================== 聚合数据 =====================

    @ApiModelProperty("视频课件列表（含本人学习进度）")
    private List<VideoStudyVO> videos;

    @ApiModelProperty("课程资料附件列表")
    private List<CourseFileVO> files;

    @ApiModelProperty("考试信息（未配置试卷时为 null）")
    private ExamInfoVO exam;

    @ApiModelProperty("是否可以开始考试")
    private Boolean canExam;
}
