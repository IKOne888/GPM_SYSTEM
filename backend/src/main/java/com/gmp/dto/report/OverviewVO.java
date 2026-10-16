package com.gmp.dto.report;

import lombok.Data;

/**
 * 报表概览指标VO
 */
@Data
public class OverviewVO {

    /** 培训总人次 */
    private Integer totalTrainees;

    /** 总培训学时 */
    private Integer totalHours;

    /** 考试通过率（百分比，如 94.5） */
    private Double passRate;

    /** 计划完成率（百分比，如 87.2） */
    private Double completionRate;
}
