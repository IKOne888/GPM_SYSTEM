package com.gmp.dto.demand;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训需求展示 VO
 * <p>
 * 在实体字段基础上，额外补充「申请部门名称」「状态中文描述」等前端直接展示所需的字段，
 * 避免前端硬编码状态与部门名称的转换逻辑。
 */
@Data
@ApiModel("培训需求展示对象")
public class DemandVO {

    /** 需求ID */
    @ApiModelProperty("需求ID")
    private Long id;

    /** 需求标题 */
    @ApiModelProperty("需求标题")
    private String title;

    /** 需求内容 */
    @ApiModelProperty("需求内容")
    private String content;

    /** 培训主题 */
    @ApiModelProperty("培训主题")
    private String trainingTopic;

    /** 培训对象 */
    @ApiModelProperty("培训对象")
    private String trainingTarget;

    /** 需求原因 / 处理意见 */
    @ApiModelProperty("需求原因/处理意见")
    private String reason;

    /** 申请部门ID */
    @ApiModelProperty("申请部门ID")
    private Long departmentId;

    /** 申请部门名称（由 Service 层批量回填） */
    @ApiModelProperty("申请部门名称")
    private String departmentName;

    /** 状态：0-待处理 1-已采纳 2-已拒绝 */
    @ApiModelProperty("状态：0-待处理 1-已采纳 2-已拒绝")
    private Integer status;

    /** 状态中文描述（由 Service 层回填，便于前端直接展示） */
    @ApiModelProperty("状态中文描述")
    private String statusText;

    /** 创建时间 */
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /** 创建人 */
    @ApiModelProperty("创建人")
    private String createBy;
}
