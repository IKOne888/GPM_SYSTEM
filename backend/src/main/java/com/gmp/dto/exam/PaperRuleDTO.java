package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 试卷组卷规则DTO（随机组卷使用）
 * <p>
 * 例：抽取 20 道中等难度单选题、每题 2 分。
 */
@Data
@ApiModel("组卷规则")
public class PaperRuleDTO {

    @ApiModelProperty("题型：single/multiple/judge/fill")
    private String type;

    @ApiModelProperty("难度：easy/medium/hard，可为空表示不限")
    private String difficulty;

    @ApiModelProperty("抽取数量")
    private Integer count;

    @ApiModelProperty("每题分值")
    private Integer score;
}
