package com.gmp.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 名称-数值对VO（用于饼图/环形图/玫瑰图）
 */
@Data
@AllArgsConstructor
public class NameValueVO {

    /** 名称 */
    private String name;

    /** 数值 */
    private Integer value;
}
