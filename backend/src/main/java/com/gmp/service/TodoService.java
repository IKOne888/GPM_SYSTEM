package com.gmp.service;

import com.gmp.dto.todo.TodoOverviewVO;

/**
 * 统一待办中心 Service 接口
 * <p>
 * 聚合「待审核任务 / 待处理需求 / 待审批计划」三类待办，
 * 统计口径按当前登录用户角色自适应（部门负责人仅本部门待审核任务；管理员可见全部）。
 */
public interface TodoService {

    /**
     * 查询统一待办聚合数据（统计 + 各类 Top5 明细）
     *
     * @return 待办聚合数据
     */
    TodoOverviewVO overview();
}