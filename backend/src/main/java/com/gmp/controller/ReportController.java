package com.gmp.controller;

import com.alibaba.excel.EasyExcel;
import com.gmp.common.Result;
import com.gmp.dto.report.DepartmentStatExportVO;
import com.gmp.dto.report.ReportSummaryVO;
import com.gmp.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 统计报表Controller
 */
@Api(tags = "统计报表")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    /** 报表Service */
    private final ReportService reportService;

    @ApiOperation("查询报表汇总数据")
    @GetMapping("/summary")
    public Result<ReportSummaryVO> summary(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Long departmentId) {
        return Result.success(reportService.getSummary(year, departmentId));
    }

    @ApiOperation("导出部门培训统计Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String year,
                       @RequestParam(required = false) Long departmentId) throws IOException {
        // 查询导出的部门统计数据
        List<DepartmentStatExportVO> dataList = reportService.exportDepartmentStats(year, departmentId);

        // 设置响应头，指定下载文件名
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("部门培训统计报表", StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 使用EasyExcel写出数据
        EasyExcel.write(response.getOutputStream(), DepartmentStatExportVO.class)
                .sheet("部门培训统计")
                .doWrite(dataList);
    }
}
