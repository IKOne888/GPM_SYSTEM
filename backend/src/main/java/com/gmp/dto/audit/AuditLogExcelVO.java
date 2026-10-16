package com.gmp.dto.audit;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 审计日志导出模型（EasyExcel）
 */
@Data
public class AuditLogExcelVO {

    @ExcelProperty("日志ID")
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty("操作模块")
    @ColumnWidth(14)
    private String module;

    @ExcelProperty("操作类型")
    @ColumnWidth(12)
    private String action;

    @ExcelProperty("操作描述")
    @ColumnWidth(24)
    private String description;

    @ExcelProperty("操作人")
    @ColumnWidth(14)
    private String operator;

    @ExcelProperty("操作IP")
    @ColumnWidth(18)
    private String operatorIp;

    @ExcelProperty("请求方式")
    @ColumnWidth(12)
    private String requestMethod;

    @ExcelProperty("请求URL")
    @ColumnWidth(40)
    private String requestUrl;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("耗时(ms)")
    @ColumnWidth(12)
    private Integer duration;

    @ExcelProperty("操作时间")
    @ColumnWidth(22)
    private String operateTime;
}
