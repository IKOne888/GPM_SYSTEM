package com.gmp.dto.report;

import lombok.Data;
import java.util.List;

/**
 * 图表序列数据VO（用于条形图/折线图）
 */
@Data
public class ChartSeriesVO {

    /** X轴/类别轴标签，如部门名或月份 */
    private List<String> labels;

    /** 对应数值 */
    private List<Double> values;
}
