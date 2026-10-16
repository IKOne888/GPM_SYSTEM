package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 任务考试信息 VO
 * <p>
 * 供「考试须知」页面展示试卷基本参数、剩余考试次数与上次成绩。
 */
@Data
@ApiModel("任务考试信息")
public class ExamInfoVO {

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

    @ApiModelProperty("题目数量")
    private Integer questionCount;

    @ApiModelProperty("最大考试次数")
    private Integer maxRetry;

    @ApiModelProperty("已使用考试次数")
    private Integer usedRetry;

    @ApiModelProperty("剩余考试次数")
    private Integer remainRetry;

    @ApiModelProperty("上次考试成绩（无记录时为 null）")
    private Integer lastScore;

    @ApiModelProperty("上次是否通过：0-未通过 1-已通过")
    private Integer lastPass;

    @ApiModelProperty("是否可以开始考试")
    private Boolean canExam;

    @ApiModelProperty("不可考试的原因说明（可考试时为空）")
    private String reason;
}
