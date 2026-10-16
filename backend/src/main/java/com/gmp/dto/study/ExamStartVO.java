package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开始考试 / 取回考试 VO
 * <p>
 * 「开始考试」与「取回考试中数据」共用该模型：返回试卷基本参数、交卷截止时间、
 * 题目列表（不含答案）以及已作答内容（用于刷新后恢复答题状态）。
 */
@Data
@ApiModel("考试进行中数据")
public class ExamStartVO {

    @ApiModelProperty("考试记录ID")
    private Long recordId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("试卷ID")
    private Long paperId;

    @ApiModelProperty("试卷名称")
    private String paperName;

    @ApiModelProperty("考试时长（分钟）")
    private Integer duration;

    @ApiModelProperty("总分")
    private Integer totalScore;

    @ApiModelProperty("及格分")
    private Integer passScore;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("交卷截止时间（开始时间 + 考试时长）")
    private LocalDateTime deadline;

    @ApiModelProperty("剩余答题时间（秒）")
    private Long remainSeconds;

    @ApiModelProperty("第几次考试（1 表示首次）")
    private Integer retryCount;

    @ApiModelProperty("题目列表（不含答案与解析）")
    private List<ExamQuestionVO> questions;

    @ApiModelProperty("已作答内容（刷新后恢复用）")
    private List<ExamSubmitDTO.AnswerItem> savedAnswers;
}
