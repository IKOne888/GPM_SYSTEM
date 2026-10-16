package com.gmp.dto.study;

import com.gmp.dto.exam.QuestionOptionDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 成绩单逐题解析项 VO
 * <p>
 * 交卷判分后返回，包含考生答案、正确答案、对错与解析。
 */
@Data
@ApiModel("逐题解析")
public class ExamResultItemVO {

    @ApiModelProperty("题目ID")
    private Long id;

    @ApiModelProperty("题型：1-单选 2-多选 3-判断 4-填空 5-简答")
    private Integer questionType;

    @ApiModelProperty("题型名称")
    private String typeName;

    @ApiModelProperty("题目内容")
    private String content;

    @ApiModelProperty("选项列表（单选/多选）")
    private List<QuestionOptionDTO> options;

    @ApiModelProperty("本题满分")
    private Integer score;

    @ApiModelProperty("考生答案")
    private String userAnswer;

    @ApiModelProperty("正确答案")
    private String correctAnswer;

    @ApiModelProperty("是否答对：0-错误 1-正确")
    private Integer isCorrect;

    @ApiModelProperty("本题得分")
    private Integer earnedScore;

    @ApiModelProperty("答案解析")
    private String analysis;

    @ApiModelProperty("题目序号（从1开始）")
    private Integer sort;
}
