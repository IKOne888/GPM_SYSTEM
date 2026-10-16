package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目列表返回VO
 * <p>
 * 与前端字段保持一致：type/difficulty 由数字枚举转换为字符串，options 由 JSON 解析为数组。
 */
@Data
@ApiModel("题目信息")
public class QuestionVO {

    @ApiModelProperty("题目ID")
    private Long id;

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("题型：single/multiple/judge/fill/short_answer")
    private String type;

    @ApiModelProperty("题目内容")
    private String content;

    @ApiModelProperty("选项列表")
    private List<QuestionOptionDTO> options;

    @ApiModelProperty("正确答案")
    private String answer;

    @ApiModelProperty("答案解析")
    private String analysis;

    @ApiModelProperty("分值")
    private Integer score;

    @ApiModelProperty("难度：easy/medium/hard")
    private String difficulty;

    @ApiModelProperty("课程分类编码")
    private String category;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
