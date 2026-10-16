package com.gmp.dto.archive;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工培训档案概览 VO（管理端视角）
 * <p>
 * 用于培训档案页面顶部概览卡，汇总员工基础信息 + 培训/考试/证书统计。
 */
@Data
@ApiModel("员工培训档案概览")
public class ArchiveOverviewVO {

    @ApiModelProperty("员工ID")
    private Long userId;

    @ApiModelProperty("员工姓名")
    private String realName;

    @ApiModelProperty("登录账号")
    private String username;

    @ApiModelProperty("部门名称")
    private String departmentName;

    @ApiModelProperty("岗位名称")
    private String positionName;

    @ApiModelProperty("入职时间 / 最后登录时间")
    private LocalDateTime lastLoginTime;

    // --- 培训统计 ---

    @ApiModelProperty("累计学习时长（小时，保留1位小数）")
    private BigDecimal totalStudyHours;

    @ApiModelProperty("学习记录条数")
    private Long studyRecordCount;

    // --- 考试统计 ---

    @ApiModelProperty("已参加考试次数")
    private Long examCount;

    @ApiModelProperty("已通过考试次数")
    private Long passedExamCount;

    @ApiModelProperty("平均得分（仅已通过考试的平均分，可空）")
    private BigDecimal avgPassScore;

    // --- 证书统计 ---

    @ApiModelProperty("有效证书数")
    private Long validCertCount;

    @ApiModelProperty("临期证书数（≤30天）")
    private Long expiringCertCount;

    @ApiModelProperty("过期证书数")
    private Long expiredCertCount;
}
