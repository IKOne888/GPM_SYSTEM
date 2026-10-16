package com.gmp.dto.report;

import lombok.Data;

/**
 * 部门培训统计明细VO
 */
@Data
public class DepartmentStatVO {

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 总人数 */
    private Integer totalEmployees;

    /** 参训人数 */
    private Integer trainedCount;

    /** 培训覆盖率（百分比） */
    private Double trainingRate;

    /** 总学时 */
    private Integer totalHours;

    /** 人均学时 */
    private Double avgHours;

    /** 考试通过率（百分比） */
    private Double examPassRate;

    /** 计划完成率（百分比） */
    private Double planCompletionRate;

    /** 逾期任务数 */
    private Integer overdueTasks;
}
