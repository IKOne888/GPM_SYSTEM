package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 试卷新增/修改DTO
 * <p>
 * 前端 status 使用字符串（draft/published/archived），后端映射为 tinyint（0/1/2）。
 */
@Data
@ApiModel("试卷保存参数")
public class PaperSaveDTO {

    @ApiModelProperty("试卷ID（新增时为空，修改时必填）")
    private Long id;

    @ApiModelProperty(value = "试卷编号", required = true)
    @NotBlank(message = "试卷编号不能为空")
    private String code;

    @ApiModelProperty(value = "试卷名称", required = true)
    @NotBlank(message = "试卷名称不能为空")
    private String name;

    @ApiModelProperty("绑定课程ID（学员端据此按课程匹配试卷，可为空表示不绑定课程）")
    private Long courseId;

    @ApiModelProperty(value = "总分", required = true)
    @NotNull(message = "总分不能为空")
    private Integer totalScore;

    @ApiModelProperty(value = "及格分", required = true)
    @NotNull(message = "及格分不能为空")
    private Integer passScore;

    @ApiModelProperty("考试时长（分钟）")
    private Integer duration;

    @ApiModelProperty("组卷方式：manual-手动选题 random-随机组卷")
    private String compositionType;

    @ApiModelProperty("组卷规则列表（随机组卷）")
    private List<PaperRuleDTO> rules;

    @ApiModelProperty("状态：draft/published/archived")
    private String status;
}
