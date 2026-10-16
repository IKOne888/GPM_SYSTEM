package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试成绩单 VO
 * <p>
 * 交卷判分后返回总体成绩 + 逐题解析。
 */
@Data
@ApiModel("考试成绩单")
public class ExamResultVO {

    @ApiModelProperty("考试记录ID")
    private Long recordId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("试卷ID")
    private Long paperId;

    @ApiModelProperty("试卷名称")
    private String paperName;

    @ApiModelProperty("得分")
    private Integer score;

    @ApiModelProperty("总分")
    private Integer totalScore;

    @ApiModelProperty("及格分")
    private Integer passScore;

    @ApiModelProperty("是否通过：0-未通过 1-已通过")
    private Integer pass;

    @ApiModelProperty("状态：0-考试中 1-已提交 2-已批阅")
    private Integer status;

    @ApiModelProperty("第几次考试")
    private Integer retryCount;

    @ApiModelProperty("提交时间")
    private LocalDateTime submitTime;

    @ApiModelProperty("答题时长（秒）")
    private Integer duration;

    @ApiModelProperty("题目总数")
    private Integer questionCount;

    @ApiModelProperty("答对题目数")
    private Integer correctCount;

    @ApiModelProperty("逐题解析")
    private List<ExamResultItemVO> items;
}
