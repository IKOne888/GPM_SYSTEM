package com.gmp.service;

import com.gmp.dto.report.ReportSummaryVO;
import com.gmp.dto.report.DepartmentStatExportVO;

import java.util.List;

/**
 * 统计报表Service接口
 */
public interface ReportService {

    /**
     * 查询报表汇总数据
     *
     * @param year         统计年度（如 2026）
     * @param departmentId 部门ID（为空表示全部部门）
     * @return 报表汇总VO
     */
    ReportSummaryVO getSummary(String year, Long departmentId);

    /**
     * 导出部门培训统计Excel数据
     *
     * @param year         统计年度（如 2026）
     * @param departmentId 部门ID（为空表示全部部门）
     * @return 导出数据列表
     */
    List<DepartmentStatExportVO> exportDepartmentStats(String year, Long departmentId);
}
