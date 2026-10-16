package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 题目新增/修改DTO
 * <p>
 * 前端字段与后端字段的映射：
 * type（single/multiple/judge/fill/short_answer）→ questionType（1/2/3/4/5）；
 * difficulty（easy/medium/hard）→ difficulty（1/2/3）。
 * 映射统一由 Service 层完成，Controller 仅做非空校验。
 */
@Data
@ApiModel("题目保存参数")
public class QuestionSaveDTO {

    @ApiModelProperty("题目ID（新增时为空，修改时必填）")
    private Long id;

    @ApiModelProperty(value = "题型：single/multiple/judge/fill/short_answer", required = true)
    @NotBlank(message = "题型不能为空")
    private String type;

    @ApiModelProperty(value = "题目内容", required = true)
    @NotBlank(message = "题目内容不能为空")
    private String content;

    @ApiModelProperty(value = "难度：easy/medium/hard", required = true)
    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @ApiModelProperty("课程分类编码")
    private String category;

    @ApiModelProperty(value = "分值", required = true)
    @NotNull(message = "分值不能为空")
    private Integer score;

    @ApiModelProperty("选项列表（单选/多选）")
    private List<QuestionOptionDTO> options;

    @ApiModelProperty("正确答案（判断/填空/简答）")
    private String answer;

    @ApiModelProperty("答案解析")
    private String analysis;
}
