package com.gmp.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.audit.AuditLogExcelVO;
import com.gmp.dto.audit.AuditLogVO;
import com.gmp.service.SysAuditLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 审计日志 Controller
 * <p>
 * 路径前缀 /audit/logs，与前端 views/audit/log/index.vue 的接口契约保持一致。
 * 审计日志为只读数据，仅提供查询与导出。
 */
@Api(tags = "审计日志管理")
@RestController
@RequestMapping("/audit/logs")
@RequiredArgsConstructor
@Validated
public class AuditLogController {

    /** 分页大小上限，防止恶意大查询拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 200;

    /** 审计日志 Service */
    private final SysAuditLogService auditLogService;

    /**
     * 分页查询审计日志
     */
    @ApiOperation("分页查询审计日志")
    @GetMapping
    public Result<Page<AuditLogVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "20")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为200") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(auditLogService.pageLogs(page, size, module, action, operator, startDate, endDate));
    }

    /**
     * 导出审计日志为 Excel
     */
    @ApiOperation("导出审计日志")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String module,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String operator,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws IOException {
        List<AuditLogExcelVO> data = auditLogService.listForExport(module, action, operator, startDate, endDate);
        // 设置 xlsx 下载响应头（文件名 URL 编码，防止中文乱码）
        String fileName = URLEncoder.encode("审计日志", StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), AuditLogExcelVO.class).sheet("审计日志").doWrite(data);
    }
}
