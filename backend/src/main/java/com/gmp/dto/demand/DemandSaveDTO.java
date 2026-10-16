package com.gmp.dto.demand;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 培训需求保存 DTO
 * <p>
 * 用于「新增需求」与「编辑需求」两个场景，编辑时由 Controller 将路径上的 id 回填到本对象。
 */
@Data
@ApiModel("培训需求保存参数")
public class DemandSaveDTO {

    /** 需求ID（编辑场景由路径参数回填，新增时为空） */
    @ApiModelProperty("需求ID（新增时无需传递）")
    private Long id;

    /** 需求标题 */
    @ApiModelProperty("需求标题")
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 200, message = "需求标题长度不能超过200")
    private String title;

    /** 需求内容 */
    @ApiModelProperty("需求内容")
    private String content;

    /** 培训主题 */
    @ApiModelProperty("培训主题")
    @Size(max = 200, message = "培训主题长度不能超过200")
    private String trainingTopic;

    /** 培训对象 */
    @ApiModelProperty("培训对象")
    @Size(max = 500, message = "培训对象长度不能超过500")
    private String trainingTarget;

    /** 需求原因 */
    @ApiModelProperty("需求原因")
    @Size(max = 1000, message = "需求原因长度不能超过1000")
    private String reason;

    /** 申请部门ID */
    @ApiModelProperty("申请部门ID")
    private Long departmentId;
}
