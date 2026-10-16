package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 错题重做判分结果 VO
 * <p>
 * 单题即时反馈：是否正确、正确答案、解析，以及该错题在重做后是否已标记为掌握。
 */
@Data
@ApiModel("错题重做结果")
public class WrongPracticeResultVO {

    @ApiModelProperty("本次是否答对")
    private Boolean correct;

    @ApiModelProperty("正确答案")
    private String correctAnswer;

    @ApiModelProperty("答案解析")
    private String analysis;

    @ApiModelProperty("重做后是否已掌握：true-已移出错题本 false-仍需复习")
    private Boolean mastered;
}
