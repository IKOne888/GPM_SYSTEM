package com.gmp.dto.study;

import com.gmp.dto.exam.QuestionOptionDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题 VO
 * <p>
 * 用于「错题本」列表展示：题目内容、选项、正确答案、解析、本人最近一次错误答案，
 * 以及答错次数与掌握状态。
 */
@Data
@ApiModel("错题信息")
public class WrongQuestionVO {

    @ApiModelProperty("错题记录ID（tr_wrong_question.id）")
    private Long id;

    @ApiModelProperty("题目ID")
    private Long questionId;

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("题型：1-单选 2-多选 3-判断 4-填空 5-简答")
    private Integer questionType;

    @ApiModelProperty("题型名称")
    private String typeName;

    @ApiModelProperty("题目内容")
    private String content;

    @ApiModelProperty("选项列表（非选择题为空）")
    private List<QuestionOptionDTO> options;

    @ApiModelProperty("正确答案")
    private String correctAnswer;

    @ApiModelProperty("答案解析")
    private String analysis;

    @ApiModelProperty("本人最近一次错误答案")
    private String myAnswer;

    @ApiModelProperty("累计答错次数")
    private Integer wrongCount;

    @ApiModelProperty("最近答错时间")
    private LocalDateTime lastWrongTime;

    @ApiModelProperty("是否已掌握：0-未掌握 1-已掌握")
    private Integer mastered;
}
