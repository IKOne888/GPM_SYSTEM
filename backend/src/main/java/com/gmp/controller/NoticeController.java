package com.gmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.Result;
import com.gmp.dto.notice.NoticePublishDTO;
import com.gmp.dto.notice.NoticeTargetVO;
import com.gmp.dto.notice.NoticeVO;
import com.gmp.service.NoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 通知公告管理 Controller
 * <p>
 * 承载管理员手工发布公告（全员 / 按部门 / 按岗位）与发布历史查询。
 * 权限：仅 SUPER_ADMIN / QA_MANAGER / TRAINING_ADMIN（在 SaTokenConfig 中收紧，排除部门负责人）。
 */
@Api(tags = "通知公告")
@RestController
@RequestMapping("/system/notices")
@RequiredArgsConstructor
public class NoticeController {

    /** 分页大小上限，防止恶意大查询 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 通知公告 Service */
    private final NoticeService noticeService;

    /**
     * 发布公告
     */
    @ApiOperation("发布公告")
    @PostMapping
    public Result<Integer> publish(@Valid @RequestBody NoticePublishDTO dto) {
        // 返回实际触达人数，并在 message 中回显，便于前端直接提示
        int count = noticeService.publish(dto);
        Result<Integer> result = Result.success(count);
        result.setMessage("已发送给 " + count + " 人");
        return result;
    }

    /**
     * 公告发布历史（按标题+正文聚合，倒序）
     */
    @ApiOperation("公告发布历史")
    @GetMapping
    public Result<Page<NoticeVO>> history(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为100") Integer size) {
        return Result.success(noticeService.pageNotices(page, size));
    }

    /**
     * 可选接收对象（部门 / 岗位下拉）
     */
    @ApiOperation("公告接收对象选项")
    @GetMapping("/targets")
    public Result<NoticeTargetVO> targets() {
        return Result.success(noticeService.targets());
    }
}