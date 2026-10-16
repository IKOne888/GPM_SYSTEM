package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 题目选项DTO
 * <p>
 * 仅单选/多选题使用；填空题、简答题、判断题不使用该结构。
 * 前端通过 isAnswer 标记该选项是否为正确答案。
 */
@Data
@ApiModel("题目选项")
public class QuestionOptionDTO {

    @ApiModelProperty("选项内容")
    private String text;

    @ApiModelProperty("是否为正确答案")
    private Boolean isAnswer;
}
