package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷列表返回VO
 * <p>
 * duration 对应实体 examDuration；status 由数字枚举转换为字符串（draft/published/archived）。
 */
@Data
@ApiModel("试卷信息")
public class PaperVO {

    @ApiModelProperty("试卷ID")
    private Long id;

    @ApiModelProperty("试卷编号")
    private String code;

    @ApiModelProperty("试卷名称")
    private String name;

    @ApiModelProperty("绑定课程ID")
    private Long courseId;

    @ApiModelProperty("总分")
    private Integer totalScore;

    @ApiModelProperty("及格分")
    private Integer passScore;

    @ApiModelProperty("考试时长（分钟）")
    private Integer duration;

    @ApiModelProperty("题目数量")
    private Integer questionCount;

    @ApiModelProperty("组卷方式：manual/random")
    private String compositionType;

    @ApiModelProperty("组卷规则列表")
    private List<PaperRuleDTO> rules;

    @ApiModelProperty("状态：draft/published/archived")
    private String status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
