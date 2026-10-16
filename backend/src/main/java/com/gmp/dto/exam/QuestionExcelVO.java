package com.gmp.dto.exam;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 题库 Excel 导入/导出模型
 * <p>
 * 同一模型同时用于：导入模板下载、题库导出、批量导入解析。
 * 列顺序：题型、题目内容、选项A~D、正确答案、难度、分类、分值、解析。
 */
@Data
public class QuestionExcelVO {

    @ExcelProperty("题型")
    @ColumnWidth(12)
    private String type;

    @ExcelProperty("题目内容")
    @ColumnWidth(45)
    private String content;

    @ExcelProperty("选项A")
    @ColumnWidth(20)
    private String optionA;

    @ExcelProperty("选项B")
    @ColumnWidth(20)
    private String optionB;

    @ExcelProperty("选项C")
    @ColumnWidth(20)
    private String optionC;

    @ExcelProperty("选项D")
    @ColumnWidth(20)
    private String optionD;

    @ExcelProperty("正确答案")
    @ColumnWidth(15)
    private String answer;

    @ExcelProperty("难度")
    @ColumnWidth(10)
    private String difficulty;

    @ExcelProperty("分类")
    @ColumnWidth(12)
    private String category;

    @ExcelProperty("分值")
    @ColumnWidth(8)
    private Integer score;

    @ExcelProperty("解析")
    @ColumnWidth(30)
    private String analysis;
}
