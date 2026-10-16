package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 岗位培训矩阵行VO
 * <p>
 * 用于岗位培训矩阵配置页面：列出全部课程及其在该岗位下的培训要求。
 */
@Data
@ApiModel("岗位培训矩阵行")
public class PositionMatrixVO {

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("课程分类名称")
    private String category;

    @ApiModelProperty("是否必修")
    private Boolean required;

    @ApiModelProperty("是否选修")
    private Boolean elective;

    @ApiModelProperty("培训频次")
    private String frequency;
}
