package com.gmp.dto.report;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 部门培训统计导出Excel模型
 * 用于报表导出功能
 */
@Data
public class DepartmentStatExportVO {

    /** 部门名称 */
    @ExcelProperty("部门")
    private String departmentName;

    /** 总人数 */
    @ExcelProperty("总人数")
    private Integer totalEmployees;

    /** 参训人数 */
    @ExcelProperty("参训人数")
    private Integer trainedCount;

    /** 培训覆盖率 */
    @ExcelProperty("培训覆盖率(%)")
    private Double trainingRate;

    /** 总学时 */
    @ExcelProperty("总学时")
    private Integer totalHours;

    /** 人均学时 */
    @ExcelProperty("人均学时")
    private Double avgHours;

    /** 考试通过率 */
    @ExcelProperty("考试通过率(%)")
    private Double examPassRate;

    /** 计划完成率 */
    @ExcelProperty("计划完成率(%)")
    private Double planCompletionRate;

    /** 逾期任务 */
    @ExcelProperty("逾期任务数")
    private Integer overdueTasks;
}
