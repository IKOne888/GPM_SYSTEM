package com.gmp.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.dto.todo.TodoGroupVO;
import com.gmp.dto.todo.TodoItemVO;
import com.gmp.dto.todo.TodoOverviewVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysUser;
import com.gmp.entity.TrTrainingDemand;
import com.gmp.entity.TrTrainingPlan;
import com.gmp.entity.TrTrainingTask;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.TrTrainingDemandMapper;
import com.gmp.mapper.TrTrainingPlanMapper;
import com.gmp.mapper.TrTrainingTaskMapper;
import com.gmp.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统一待办中心 Service 实现类
 * <p>
 * 三类待办的数据来源：
 * 待审核任务 - tr_training_task.review_status；待处理需求 - tr_training_demand.status=0；
 * 待审批计划 - tr_training_plan.status=1。
 * <p>
 * 角色口径：
 * 部门负责人仅统计本部门「待主管审核」任务，需求与计划计数为 0；
 * QA 复核仅统计全局「待QA复核」任务；系统管理员/培训管理员统计全部待审核任务与全部需求、计划。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    /** 待办明细每类展示条数 */
    private static final int TOP_SIZE = 5;

    /** 角色：系统管理员 */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    /** 角色：质量管理员（QA） */
    private static final String ROLE_QA_MANAGER = "QA_MANAGER";
    /** 角色：培训管理员 */
    private static final String ROLE_TRAINING_ADMIN = "TRAINING_ADMIN";
    /** 角色：部门负责人 */
    private static final String ROLE_DEPT_LEADER = "DEPT_LEADER";

    /** 审核状态：待主管审核 */
    private static final int REVIEW_PENDING_LEADER = 0;
    /** 审核状态：待QA复核 */
    private static final int REVIEW_PENDING_QA = 1;

    /** 需求状态：待处理 */
    private static final int DEMAND_PENDING = 0;
    /** 计划状态：待审批 */
    private static final int PLAN_PENDING = 1;

    /** 任务 Mapper */
    private final TrTrainingTaskMapper taskMapper;
    /** 需求 Mapper */
    private final TrTrainingDemandMapper demandMapper;
    /** 计划 Mapper */
    private final TrTrainingPlanMapper planMapper;
    /** 用户 Mapper（回填学员姓名/部门） */
    private final SysUserMapper sysUserMapper;
    /** 部门 Mapper（回填部门名称） */
    private final SysDepartmentMapper sysDepartmentMapper;

    @Override
    public TodoOverviewVO overview() {
        List<String> roles = StpUtil.getRoleList();
        // 是否仅具备部门负责人角色（不具备任何管理员角色）→ 启用部门数据隔离
        boolean leaderOnly = roles.contains(ROLE_DEPT_LEADER) && !hasAdminRole(roles);
        // 是否仅具备 QA 角色（非系统管理员、非培训管理员）→ 只看复核阶段
        boolean qaOnly = roles.contains(ROLE_QA_MANAGER)
                && !roles.contains(ROLE_SUPER_ADMIN) && !roles.contains(ROLE_TRAINING_ADMIN);
        // 是否管理员（可看待处理需求与待审批计划）
        boolean admin = hasAdminRole(roles);

        TodoOverviewVO overview = new TodoOverviewVO();
        overview.setReviewPending(buildReviewGroup(leaderOnly, qaOnly));
        overview.setDemandPending(admin ? buildDemandGroup() : emptyGroup());
        overview.setPlanPending(admin ? buildPlanGroup() : emptyGroup());

        long total = overview.getReviewPending().getCount()
                + overview.getDemandPending().getCount()
                + overview.getPlanPending().getCount();
        overview.setTotal(total);
        return overview;
    }

    /**
     * 判断是否具备任一管理员角色（系统管理员 / QA / 培训管理员）
     *
     * @param roles 角色编码列表
     * @return 具备返回 true
     */
    private boolean hasAdminRole(List<String> roles) {
        return roles.contains(ROLE_SUPER_ADMIN) || roles.contains(ROLE_QA_MANAGER) || roles.contains(ROLE_TRAINING_ADMIN);
    }

    /**
     * 构建空分组（部门负责人无需求/计划待办）
     *
     * @return 计数为 0、明细为空的待办分组
     */
    private TodoGroupVO emptyGroup() {
        TodoGroupVO group = new TodoGroupVO();
        group.setCount(0L);
        group.setItems(new ArrayList<>());
        return group;
    }

    /**
     * 构建「待审核任务」分组
     *
     * @param leaderOnly 是否仅部门负责人（本部门 + 待主管审核）
     * @param qaOnly     是否仅 QA（全局 + 待QA复核）
     * @return 待办分组
     */
    private TodoGroupVO buildReviewGroup(boolean leaderOnly, boolean qaOnly) {
        // 总数统计
        Long count = taskMapper.selectCount(reviewWrapper(leaderOnly, qaOnly));

        // Top5 明细（按创建时间倒序，最新待办优先）
        LambdaQueryWrapper<TrTrainingTask> itemWrapper = reviewWrapper(leaderOnly, qaOnly);
        itemWrapper.orderByDesc(TrTrainingTask::getCreateTime);
        Page<TrTrainingTask> page = taskMapper.selectPage(new Page<>(1, TOP_SIZE), itemWrapper);
        List<TrTrainingTask> tasks = page.getRecords();
        // 批量取学员姓名与部门，避免 N+1
        Map<Long, SysUser> userMap = loadUsers(tasks.stream().map(TrTrainingTask::getUserId).collect(Collectors.toList()));
        Map<Long, String> deptNameMap = loadDepartmentNames(userMap.values().stream()
                .map(SysUser::getDepartmentId).collect(Collectors.toList()));

        List<TodoItemVO> items = tasks.stream().map(t -> {
            TodoItemVO item = new TodoItemVO();
            item.setId(t.getId());
            item.setTitle(t.getName());
            SysUser learner = userMap.get(t.getUserId());
            if (learner != null) {
                String deptName = deptNameMap.getOrDefault(learner.getDepartmentId(), "未分配部门");
                item.setExtra(deptName + " · 学员 " + safeName(learner));
            }
            item.setTime(t.getCreateTime());
            // 阶段标识：0-待主管审核 leader；1-待QA复核 qa
            item.setStage(Objects.equals(t.getReviewStatus(), REVIEW_PENDING_QA) ? "qa" : "leader");
            return item;
        }).collect(Collectors.toList());

        return buildGroup(count, items);
    }

    /**
     * 构造「待审核任务」查询条件（每次返回新实例，供计数与明细分页复用）
     *
     * @param leaderOnly 是否仅部门负责人（本部门 + 待主管审核）
     * @param qaOnly     是否仅 QA（全局 + 待QA复核）
     * @return 查询条件
     */
    private LambdaQueryWrapper<TrTrainingTask> reviewWrapper(boolean leaderOnly, boolean qaOnly) {
        LambdaQueryWrapper<TrTrainingTask> wrapper = new LambdaQueryWrapper<>();
        if (leaderOnly) {
            // 部门负责人：仅本部门待主管审核任务
            wrapper.eq(TrTrainingTask::getReviewStatus, REVIEW_PENDING_LEADER);
            List<Long> deptUserIds = currentDepartmentUserIds();
            if (deptUserIds.isEmpty()) {
                wrapper.eq(TrTrainingTask::getUserId, -1L);
            } else {
                wrapper.in(TrTrainingTask::getUserId, deptUserIds);
            }
        } else if (qaOnly) {
            // QA 复核：全局待QA复核任务
            wrapper.eq(TrTrainingTask::getReviewStatus, REVIEW_PENDING_QA);
        } else {
            // 管理员：待主管审核 + 待QA复核
            wrapper.in(TrTrainingTask::getReviewStatus, REVIEW_PENDING_LEADER, REVIEW_PENDING_QA);
        }
        return wrapper;
    }

    /**
     * 构建「待处理需求」分组（全部待处理需求，按登记时间倒序）
     *
     * @return 待办分组
     */
    private TodoGroupVO buildDemandGroup() {
        LambdaQueryWrapper<TrTrainingDemand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrTrainingDemand::getStatus, DEMAND_PENDING);
        Long count = demandMapper.selectCount(wrapper);

        LambdaQueryWrapper<TrTrainingDemand> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(TrTrainingDemand::getStatus, DEMAND_PENDING)
                .orderByDesc(TrTrainingDemand::getCreateTime);
        Page<TrTrainingDemand> page = demandMapper.selectPage(new Page<>(1, TOP_SIZE), itemWrapper);

        List<TodoItemVO> items = page.getRecords().stream().map(d -> {
            TodoItemVO item = new TodoItemVO();
            item.setId(d.getId());
            item.setTitle(d.getTitle());
            item.setExtra("提交人 " + (d.getCreateBy() == null ? "未知" : d.getCreateBy()));
            item.setTime(d.getCreateTime());
            return item;
        }).collect(Collectors.toList());

        return buildGroup(count, items);
    }

    /**
     * 构建「待审批计划」分组（全部待审批计划，按提交时间倒序）
     *
     * @return 待办分组
     */
    private TodoGroupVO buildPlanGroup() {
        LambdaQueryWrapper<TrTrainingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrTrainingPlan::getStatus, PLAN_PENDING);
        Long count = planMapper.selectCount(wrapper);

        LambdaQueryWrapper<TrTrainingPlan> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(TrTrainingPlan::getStatus, PLAN_PENDING)
                .orderByDesc(TrTrainingPlan::getSubmitTime);
        Page<TrTrainingPlan> page = planMapper.selectPage(new Page<>(1, TOP_SIZE), itemWrapper);

        List<TodoItemVO> items = page.getRecords().stream().map(p -> {
            TodoItemVO item = new TodoItemVO();
            item.setId(p.getId());
            item.setTitle(p.getName());
            item.setExtra("提交人 " + (p.getCreateBy() == null ? "未知" : p.getCreateBy()));
            item.setTime(p.getSubmitTime());
            return item;
        }).collect(Collectors.toList());

        return buildGroup(count, items);
    }

    /**
     * 组装待办分组
     *
     * @param count 总数
     * @param items 明细
     * @return 待办分组
     */
    private TodoGroupVO buildGroup(Long count, List<TodoItemVO> items) {
        TodoGroupVO group = new TodoGroupVO();
        group.setCount(count == null ? 0L : count);
        group.setItems(items);
        return group;
    }

    /**
     * 查询当前登录用户所属部门的全部用户ID（部门负责人数据隔离用）
     *
     * @return 同部门用户ID列表，无部门时返回空列表
     */
    private List<Long> currentDepartmentUserIds() {
        SysUser current = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (current == null || current.getDepartmentId() == null) {
            return Collections.emptyList();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getDepartmentId, current.getDepartmentId()))
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }

    /**
     * 批量加载用户（去重、过滤空值）
     *
     * @param userIds 用户ID列表（可含空值）
     * @return 用户ID → 用户实体
     */
    private Map<Long, SysUser> loadUsers(List<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    /**
     * 批量加载部门名称（去重、过滤空值）
     *
     * @param deptIds 部门ID列表（可含空值）
     * @return 部门ID → 部门名称
     */
    private Map<Long, String> loadDepartmentNames(List<Long> deptIds) {
        List<Long> distinct = deptIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        return sysDepartmentMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
    }

    /**
     * 取用户展示名（优先真实姓名，其次用户名）
     *
     * @param user 用户实体
     * @return 展示名
     */
    private String safeName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isEmpty()) {
            return user.getRealName();
        }
        return user.getUsername();
    }
}