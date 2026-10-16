package com.gmp.dto.report;

import lombok.Data;
import java.util.List;

/**
 * 报表汇总返回VO
 * 对应前端报表页一次接口调用所需的全部统计数据
 */
@Data
public class ReportSummaryVO {

    /** 概览指标（顶部4张卡片） */
    private OverviewVO overview;

    /** 部门培训统计明细（底部表格） */
    private List<DepartmentStatVO> departmentStats;

    /** 各部门培训完成率（条形图：部门名+完成率） */
    private ChartSeriesVO deptCompletion;

    /** 月度培训学时（折线图：月份+学时） */
    private ChartSeriesVO monthHours;

    /** 考试成绩分布（环形图） */
    private List<NameValueVO> scoreDistribution;

    /** 课程分类占比（玫瑰图） */
    private List<NameValueVO> categoryDistribution;
}
