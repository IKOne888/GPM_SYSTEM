package com.gmp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程列表返回VO（含分类名称）
 */
@Data
@ApiModel("课程信息")
public class CourseVO {

    @ApiModelProperty("课程ID")
    private Long id;

    @ApiModelProperty("课程名称")
    private String name;

    @ApiModelProperty("课程编码")
    private String code;

    @ApiModelProperty("课程分类ID")
    private Long categoryId;

    @ApiModelProperty("课程分类名称")
    private String categoryName;

    @ApiModelProperty("所属部门ID")
    private Long departmentId;

    @ApiModelProperty("所属部门名称")
    private String departmentName;

    @ApiModelProperty("适用岗位ID")
    private Long positionId;

    @ApiModelProperty("适用岗位名称")
    private String positionName;

    @ApiModelProperty("SOP编号")
    private String sopCode;

    @ApiModelProperty("SOP版本号")
    private String sopVersion;

    @ApiModelProperty("培训目标")
    private String trainingObjective;

    @ApiModelProperty("培训学时")
    private BigDecimal trainingHours;

    @ApiModelProperty("及格分数")
    private Integer passScore;

    @ApiModelProperty("复训周期（天）")
    private Integer retrainPeriod;

    @ApiModelProperty("状态：0-停用 1-启用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    /**
     * 从课程实体构建
     */
    public static CourseVO from(com.gmp.entity.TrCourse course) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setCode(course.getCode());
        vo.setCategoryId(course.getCategoryId());
        // 部门/岗位：VO 直接冗余回填ID，名称由 Service 层批量查询后填充
        vo.setDepartmentId(course.getDepartmentId());
        vo.setPositionId(course.getPositionId());
        vo.setSopCode(course.getSopCode());
        vo.setSopVersion(course.getSopVersion());
        vo.setTrainingObjective(course.getTrainingObjective());
        vo.setTrainingHours(course.getTrainingHours());
        vo.setPassScore(course.getPassScore());
        vo.setRetrainPeriod(course.getRetrainPeriod());
        vo.setStatus(course.getStatus());
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());
        return vo;
    }
}
