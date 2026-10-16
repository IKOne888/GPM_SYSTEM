package com.gmp.dto.demand;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 学员端培训需求提交 DTO
 * <p>
 * 与管理员端 {@link DemandSaveDTO} 区分：学员端不允许指定部门与处理状态，
 * 部门由服务端取当前登录用户的所属部门，状态固定为「待处理」，防止学员自封结论。
 */
@Data
@ApiModel("学员端培训需求提交参数")
public class DemandSubmitDTO {

    /** 需求标题（必填，≤100） */
    @ApiModelProperty(value = "需求标题", required = true)
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 100, message = "需求标题长度不能超过100")
    private String title;

    /** 需求内容（必填，≤1000） */
    @ApiModelProperty(value = "需求内容", required = true)
    @NotBlank(message = "需求内容不能为空")
    @Size(max = 1000, message = "需求内容长度不能超过1000")
    private String content;

    /** 培训主题（选填，≤100） */
    @ApiModelProperty("培训主题")
    @Size(max = 100, message = "培训主题长度不能超过100")
    private String trainingTopic;

    /** 培训对象（选填，≤200） */
    @ApiModelProperty("培训对象")
    @Size(max = 200, message = "培训对象长度不能超过200")
    private String trainingTarget;
}