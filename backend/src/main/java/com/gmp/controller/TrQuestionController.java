package com.gmp.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.exam.ImportResultVO;
import com.gmp.dto.exam.QuestionExcelVO;
import com.gmp.dto.exam.QuestionSaveDTO;
import com.gmp.dto.exam.QuestionVO;
import com.gmp.service.TrQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 题库管理Controller
 * <p>
 * 路径前缀 /exam/questions，与前端 views/exam/question/index.vue 的接口契约保持一致。
 */
@Api(tags = "题库管理")
@RestController
@RequestMapping("/exam/questions")
@RequiredArgsConstructor
@Validated
public class TrQuestionController {

    /** 分页大小上限，防止恶意大查询拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 题库Service */
    private final TrQuestionService questionService;

    /**
     * 分页查询题目
     */
    @ApiOperation("分页查询题目")
    @GetMapping
    public Result<Page<QuestionVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category) {
        // 直接返回分页结果，前端读取 data.records / data.total
        return Result.success(questionService.pageQuestions(page, size, keyword, type, difficulty, category));
    }

    /**
     * 新增题目
     */
    @ApiOperation("新增题目")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody QuestionSaveDTO dto) {
        questionService.addQuestion(dto);
        return Result.success();
    }

    /**
     * 修改题目
     */
    @ApiOperation("修改题目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "题目ID不能为空") Long id,
                               @Valid @RequestBody QuestionSaveDTO dto) {
        // 以路径参数为准，防止篡改
        dto.setId(id);
        questionService.updateQuestion(dto);
        return Result.success();
    }

    /**
     * 删除题目（逻辑删除）
     */
    @ApiOperation("删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "题目ID不能为空") Long id) {
        questionService.removeById(id);
        return Result.success();
    }

    /**
     * Excel 批量导入题目
     */
    @ApiOperation("Excel批量导入题目")
    @PostMapping("/import")
    public Result<ImportResultVO> importQuestions(@RequestParam("file") MultipartFile file) {
        return Result.success(questionService.importQuestions(file));
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载题目导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        writeExcel(response, "题目导入模板", questionService.templateData());
    }

    /**
     * 导出题库
     */
    @ApiOperation("导出题库")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        writeExcel(response, "题库导出", questionService.listForExport());
    }

    /**
     * 通用 Excel 写出方法（设置下载响应头并使用 EasyExcel 输出）
     *
     * @param response 响应对象
     * @param fileName 文件名（不含扩展名）
     * @param dataList 数据列表
     */
    private void writeExcel(HttpServletResponse response, String fileName, List<QuestionExcelVO> dataList) throws IOException {
        // 设置响应类型为 xlsx
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 文件名进行 URL 编码，避免中文乱码
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
        // 使用 EasyExcel 写出数据
        EasyExcel.write(response.getOutputStream(), QuestionExcelVO.class)
                .sheet("题库")
                .doWrite(dataList);
    }
}
