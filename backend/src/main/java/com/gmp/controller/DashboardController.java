package com.gmp.controller;

import com.gmp.common.Result;
import com.gmp.dto.todo.TodoOverviewVO;
import com.gmp.service.TodoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台 Controller
 * <p>
 * 承载管理端「统一待办中心」聚合查询；仅需登录，具体统计口径按角色在 Service 层过滤。
 */
@Api(tags = "工作台")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /** 统一待办中心 Service */
    private final TodoService todoService;

    /**
     * 统一待办聚合数据（待审核任务 / 待处理需求 / 待审批计划）
     */
    @ApiOperation("统一待办中心")
    @GetMapping("/todo")
    public Result<TodoOverviewVO> todo() {
        return Result.success(todoService.overview());
    }
}