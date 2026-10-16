package com.gmp.dto.study;

import com.gmp.dto.exam.QuestionOptionDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 在线考试题目 VO（考试中，不含答案与解析）
 * <p>
 * 为防止作弊，考试进行中返回的题目不包含 answer / analysis 字段；
 * 交卷后由 {@link ExamResultItemVO} 才返回正确答案与解析。
 */
@Data
@ApiModel("考试题目（无答案）")
public class ExamQuestionVO {

    @ApiModelProperty("题目ID")
    private Long id;

    @ApiModelProperty("题型：1-单选 2-多选 3-判断 4-填空 5-简答")
    private Integer questionType;

    @ApiModelProperty("题型名称：单选题/多选题/判断题/填空题/简答题")
    private String typeName;

    @ApiModelProperty("题目内容")
    private String content;

    @ApiModelProperty("选项列表（单选/多选返回，其余为空数组）")
    private List<QuestionOptionDTO> options;

    @ApiModelProperty("本题分值")
    private Integer score;

    @ApiModelProperty("题目序号（从1开始）")
    private Integer sort;
}
