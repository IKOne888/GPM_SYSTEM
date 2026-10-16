package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试成绩单列表项 VO
 * <p>
 * 供个人中心「成绩单」分页展示。
 */
@Data
@ApiModel("考试成绩单列表项")
public class ExamRecordVO {

    @ApiModelProperty("考试记录ID")
    private Long recordId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("试卷名称")
    private String paperName;

    @ApiModelProperty("得分")
    private Integer score;

    @ApiModelProperty("及格分")
    private Integer passScore;

    @ApiModelProperty("是否通过：0-未通过 1-已通过")
    private Integer pass;

    @ApiModelProperty("第几次考试")
    private Integer retryCount;

    @ApiModelProperty("状态：0-考试中 1-已提交 2-已批阅")
    private Integer status;

    @ApiModelProperty("提交时间")
    private LocalDateTime submitTime;

    @ApiModelProperty("答题时长（秒）")
    private Integer duration;
}
