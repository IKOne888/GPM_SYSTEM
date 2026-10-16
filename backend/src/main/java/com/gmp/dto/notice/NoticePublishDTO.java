package com.gmp.dto.notice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 通知公告发布 DTO
 * <p>
 * 接收范围 targetType：1-全员 2-按部门 3-按岗位；
 * 当 targetType 不为 1 时，targetIds 不得为空。
 */
@Data
@ApiModel("通知公告发布参数")
public class NoticePublishDTO {

    /** 公告标题（必填，≤100） */
    @ApiModelProperty(value = "公告标题", required = true)
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题长度不能超过100")
    private String title;

    /** 公告正文（必填，≤1000） */
    @ApiModelProperty(value = "公告正文", required = true)
    @NotBlank(message = "公告正文不能为空")
    @Size(max = 1000, message = "公告正文长度不能超过1000")
    private String content;

    /** 接收范围：1-全员 2-按部门 3-按岗位 */
    @ApiModelProperty(value = "接收范围：1-全员 2-按部门 3-按岗位", required = true)
    private Integer targetType;

    /** 接收对象ID集合（按部门/按岗位时必填） */
    @ApiModelProperty("接收对象ID集合（按部门/按岗位时必填）")
    private List<Long> targetIds;
}