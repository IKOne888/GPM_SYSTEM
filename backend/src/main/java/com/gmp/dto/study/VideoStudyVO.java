package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 视频学习情况 VO
 * <p>
 * 用于任务详情页的视频课件列表，携带当前学员在该视频上的学习进度与续播位置。
 */
@Data
@ApiModel("视频学习情况")
public class VideoStudyVO {

    @ApiModelProperty("视频课件ID")
    private Long id;

    @ApiModelProperty("视频名称")
    private String name;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("视频时长（秒）")
    private Integer duration;

    @ApiModelProperty("排序号")
    private Integer sort;

    @ApiModelProperty("视频播放进度（百分比）")
    private BigDecimal progress;

    @ApiModelProperty("上次播放位置（秒），用于断点续播")
    private Integer lastPosition;

    @ApiModelProperty("是否已完成：0-未完成 1-已完成")
    private Integer isCompleted;

    @ApiModelProperty("该视频累计学习时长（秒）")
    private Integer studyDuration;
}
