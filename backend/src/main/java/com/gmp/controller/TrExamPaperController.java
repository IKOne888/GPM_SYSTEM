package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.exam.PaperSaveDTO;
import com.gmp.dto.exam.PaperVO;
import com.gmp.service.TrExamPaperService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 试卷管理Controller
 * <p>
 * 路径前缀 /exam/papers，与前端 views/exam/paper/index.vue 的接口契约保持一致。
 */
@Api(tags = "试卷管理")
@RestController
@RequestMapping("/exam/papers")
@RequiredArgsConstructor
@Validated
public class TrExamPaperController {

    /** 分页大小上限 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 试卷Service */
    private final TrExamPaperService paperService;

    /**
     * 分页查询试卷
     */
    @ApiOperation("分页查询试卷")
    @GetMapping
    public Result<Page<PaperVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return Result.success(paperService.pagePapers(page, size, name, status));
    }

    /**
     * 新增试卷
     */
    @ApiOperation("新增试卷")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody PaperSaveDTO dto) {
        paperService.addPaper(dto);
        return Result.success();
    }

    /**
     * 修改试卷
     */
    @ApiOperation("修改试卷")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @NotNull(message = "试卷ID不能为空") Long id,
                               @Valid @RequestBody PaperSaveDTO dto) {
        // 以路径参数为准，防止篡改
        dto.setId(id);
        paperService.updatePaper(dto);
        return Result.success();
    }

    /**
     * 删除试卷（逻辑删除）
     */
    @ApiOperation("删除试卷")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @NotNull(message = "试卷ID不能为空") Long id) {
        paperService.removeById(id);
        return Result.success();
    }
}
