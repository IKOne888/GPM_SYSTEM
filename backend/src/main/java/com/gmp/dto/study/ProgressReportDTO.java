package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 视频学习进度上报 DTO
 * <p>
 * 前端在播放过程中按节流策略（每 15 秒 / 暂停 / 切页 / 播放结束）上报，
 * 后端据此更新 tr_video_study_record 并回填任务整体进度。
 */
@Data
@ApiModel("学习进度上报参数")
public class ProgressReportDTO {

    @ApiModelProperty(value = "培训任务ID", required = true, example = "1")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @ApiModelProperty(value = "视频课件ID", required = true, example = "1")
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    @ApiModelProperty(value = "视频播放进度（百分比，0~100）", example = "66.5")
    private BigDecimal progress;

    @ApiModelProperty(value = "上次播放位置（秒），用于断点续播", example = "120")
    private Integer lastPosition;

    @ApiModelProperty(value = "该视频累计学习时长（秒），采用绝对值覆盖，避免重复累加", example = "300")
    private Integer studyDuration;
}
