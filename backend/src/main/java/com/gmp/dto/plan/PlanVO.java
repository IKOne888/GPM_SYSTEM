package com.gmp.dto.plan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训计划列表返回 VO
 * <p>
 * status 由数字枚举转换为字符串 approvalStatus（draft/pending/approved/rejected）供前端直接使用；
 * departmentName（制定部门）与 creatorName（制定人）由 createBy 追溯用户与部门回填。
 */
@Data
@ApiModel("培训计划信息")
public class PlanVO {

    @ApiModelProperty("计划ID")
    private Long id;

    @ApiModelProperty("计划编号")
    private String code;

    @ApiModelProperty("计划名称")
    private String name;

    @ApiModelProperty("计划年度")
    private Integer year;

    @ApiModelProperty("计划类型：1-年度计划 2-专项计划")
    private Integer planType;

    @ApiModelProperty("计划描述")
    private String description;

    @ApiModelProperty("培训对象部门ID列表")
    private List<Long> targetDepartments;

    @ApiModelProperty("开始日期")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    private LocalDate endDate;

    @ApiModelProperty("状态数字：0-草稿 1-待审批 2-已通过 3-已驳回 4-已发布")
    private Integer status;

    @ApiModelProperty("审批状态（前端枚举）：draft/pending/approved/rejected")
    private String approvalStatus;

    @ApiModelProperty("提交审批时间")
    private LocalDateTime submitTime;

    @ApiModelProperty("审批时间")
    private LocalDateTime approveTime;

    @ApiModelProperty("审批意见")
    private String approveRemark;

    @ApiModelProperty("制定部门名称")
    private String departmentName;

    @ApiModelProperty("制定人姓名")
    private String creatorName;

    @ApiModelProperty("计划关联的课程列表")
    private List<PlanCourseVO> courses;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
