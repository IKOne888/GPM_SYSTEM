package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习记录 VO
 * <p>
 * 供个人中心「学习记录」分页展示：课程 + 视频 + 学习时长 + 进度 + 完成情况。
 */
@Data
@ApiModel("学习记录")
public class StudyRecordVO {

    @ApiModelProperty("学习记录ID")
    private Long id;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("视频课件ID")
    private Long videoId;

    @ApiModelProperty("视频名称")
    private String videoName;

    @ApiModelProperty("学习时长（秒）")
    private Integer studyDuration;

    @ApiModelProperty("视频播放进度（百分比）")
    private BigDecimal videoProgress;

    @ApiModelProperty("上次播放位置（秒）")
    private Integer lastPosition;

    @ApiModelProperty("是否完成：0-未完成 1-已完成")
    private Integer isCompleted;

    @ApiModelProperty("最后学习时间")
    private LocalDateTime lastStudyTime;
}
