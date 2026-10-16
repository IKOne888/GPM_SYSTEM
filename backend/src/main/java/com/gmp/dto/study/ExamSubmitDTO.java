package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 在线考试交卷 DTO
 * <p>
 * 提交本次考试的全部作答内容，后端按题型自动判分并回填成绩。
 */
@Data
@ApiModel("交卷参数")
public class ExamSubmitDTO {

    @ApiModelProperty(value = "作答列表", required = true)
    @NotEmpty(message = "作答内容不能为空")
    @Valid
    private List<AnswerItem> answers;

    /**
     * 单题作答项
     */
    @Data
    @ApiModel("单题作答")
    public static class AnswerItem {

        @ApiModelProperty(value = "题目ID", required = true, example = "1")
        private Long questionId;

        @ApiModelProperty(value = "考生答案（单选/多选：字母；判断：正确/错误；填空/简答：文本）", example = "A")
        private String answer;
    }
}
