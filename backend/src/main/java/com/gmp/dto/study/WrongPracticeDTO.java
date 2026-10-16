package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 错题重做提交 DTO
 * <p>
 * 用于「错题本 → 错题练习」的单题即时判分，不涉及考试记录。
 */
@Data
@ApiModel("错题重做参数")
public class WrongPracticeDTO {

    @ApiModelProperty(value = "题目ID", required = true, example = "15")
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @ApiModelProperty(value = "考生答案（单选/多选：字母；判断：正确/错误；填空/简答：文本）", example = "AB")
    private String answer;
}
