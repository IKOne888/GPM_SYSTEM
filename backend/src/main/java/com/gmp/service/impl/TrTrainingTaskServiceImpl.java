package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.task.TaskAssignDTO;
import com.gmp.dto.task.TaskReviewDTO;
import com.gmp.dto.task.TaskVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysUser;
import com.gmp.entity.SysUserRole;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrTrainingPlan;
import com.gmp.entity.TrTrainingTask;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysRoleMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.SysUserRoleMapper;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.TrTrainingPlanMapper;
import com.gmp.mapper.TrTrainingTaskMapper;
import com.gmp.service.SysMessageService;
import com.gmp.service.TrTrainingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 培训任务 Service 实现类
 * <p>
 * 负责任务的分配（按执行人拆分）、完成标记与催办，以及列表展示字段的回填。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrTrainingTaskServiceImpl extends ServiceImpl<TrTrainingTaskMapper, TrTrainingTask>
        implements TrTrainingTaskService {

    /** 用户 Mapper（回填执行人姓名、部门） */
    private final SysUserMapper sysUserMapper;
    /** 部门 Mapper（回填部门名称） */
    private final SysDepartmentMapper sysDepartmentMapper;
    /** 课程 Mapper（回填课程名称、校验课程存在） */
    private final TrCourseMapper trCourseMapper;
    /** 计划 Mapper（回填计划名称、校验计划存在） */
    private final TrTrainingPlanMapper trTrainingPlanMapper;
    /** 站内信 Service（任务分配、催办时推送给执行人） */
    private final SysMessageService sysMessageService;
    /** 用户角色关联 Mapper（回退通知管理员时按角色查用户） */
    private final SysUserRoleMapper sysUserRoleMapper;
    /** 角色 Mapper（按角色编码查角色ID） */
    private final SysRoleMapper sysRoleMapper;

    /** 任务状态：待学习 */
    private static final int STATUS_NOT_STARTED = 0;
    /** 任务状态：待考试 */
    private static final int STATUS_WAITING_EXAM = 2;
    /** 任务状态：已完成（学习与考试达标） */
    private static final int STATUS_COMPLETED = 3;

    // ==================== 审核状态常量（两级审核闭环） ====================
    /** 审核状态：待主管审核 */
    private static final int REVIEW_PENDING_LEADER = 0;
    /** 审核状态：待QA复核 */
    private static final int REVIEW_PENDING_QA = 1;
    /** 审核状态：已复核通过（归档） */
    private static final int REVIEW_APPROVED = 2;
    /** 审核状态：已退回 */
    private static final int REVIEW_REJECTED = 3;

    // ==================== 审核相关角色编码 ====================
    /** 角色：部门负责人（一级审核人） */
    private static final String ROLE_DEPT_LEADER = "DEPT_LEADER";
    /** 角色：质量管理员（二级复核人） */
    private static final String ROLE_QA_MANAGER = "QA_MANAGER";
    /** 角色：培训管理员 */
    private static final String ROLE_TRAINING_ADMIN = "TRAINING_ADMIN";
    /** 角色：系统管理员 */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /** 员工在岗状态：启用（仅启用用户可作为审核通知对象） */
    private static final int USER_STATUS_ENABLED = 1;

    @Override
    public Page<TaskVO> pageTasks(Integer pageNum, Integer pageSize, String name, String status,
                                  String assigneeName, Integer reviewStatus) {
        Page<TrTrainingTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrTrainingTask> wrapper = new LambdaQueryWrapper<>();
        // 任务名称模糊查询
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(TrTrainingTask::getName, name);
        }
        // 状态过滤（前端字符串 → 数字集合）
        List<Integer> statusList = parseStatusList(status);
        if (statusList != null) {
            wrapper.in(TrTrainingTask::getStatus, statusList);
        }
        // 审核状态过滤（用于「待我审核」页签：0-待主管审核 1-待QA复核）
        if (reviewStatus != null) {
            wrapper.eq(TrTrainingTask::getReviewStatus, reviewStatus);
        }
        // 执行人姓名模糊查询：先查用户ID，再按用户ID过滤
        if (StrUtil.isNotBlank(assigneeName)) {
            List<SysUser> matched = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .like(SysUser::getRealName, assigneeName));
            List<Long> userIds = matched.stream().map(SysUser::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                // 无匹配执行人时返回空结果，避免 IN () 语法问题
                wrapper.eq(TrTrainingTask::getUserId, -1L);
            } else {
                wrapper.in(TrTrainingTask::getUserId, userIds);
            }
        }
        // 部门数据隔离：仅部门负责人（且不具备管理员角色）时，强制只看本部门学员的任务
        if (isDepartmentScoped()) {
            List<Long> deptUserIds = findUserIdsByDepartment(currentUser().getDepartmentId());
            if (deptUserIds.isEmpty()) {
                // 本部门无在岗学员时返回空结果
                wrapper.eq(TrTrainingTask::getUserId, -1L);
            } else {
                wrapper.in(TrTrainingTask::getUserId, deptUserIds);
            }
        }
        wrapper.orderByDesc(TrTrainingTask::getCreateTime);

        Page<TrTrainingTask> entityPage = baseMapper.selectPage(page, wrapper);

        Page<TaskVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<TaskVO> voList = entityPage.getRecords().stream().map(this::toBaseVO).collect(Collectors.toList());
        fillNames(entityPage.getRecords(), voList);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(TaskAssignDTO dto) {
        // 校验课程存在
        TrCourse course = trCourseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训课程不存在");
        }
        // 校验计划存在（可选关联）
        if (dto.getPlanId() != null && trTrainingPlanMapper.selectById(dto.getPlanId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训计划不存在");
        }
        // 校验执行人存在
        List<SysUser> users = sysUserMapper.selectBatchIds(dto.getAssigneeIds());
        if (users.size() != dto.getAssigneeIds().size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的执行人");
        }

        // 按执行人拆分为多条任务记录
        LocalDateTime deadline = dto.getDeadline().atStartOfDay();
        for (Long userId : dto.getAssigneeIds()) {
            TrTrainingTask task = new TrTrainingTask();
            task.setName(dto.getName());
            task.setPlanId(dto.getPlanId());
            task.setCourseId(dto.getCourseId());
            task.setUserId(userId);
            task.setTaskType(1);
            task.setDescription(dto.getDescription());
            task.setStatus(STATUS_NOT_STARTED);
            task.setDeadline(deadline);
            task.setVideoProgress(BigDecimal.ZERO);
            task.setRetryCount(0);
            baseMapper.insert(task);
            // 任务分配成功后立即推送站内信，学员端消息中心与铃铛可即时看到
            sysMessageService.send(userId, SysMessageService.TYPE_TASK,
                    "新培训任务通知",
                    "您有新的培训任务《" + dto.getName() + "》（课程：" + course.getName()
                            + "），请于 " + dto.getDeadline() + " 前完成学习与考试。",
                    task.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, String remark) {
        // 权限校验：免考确认完成属高风险操作，仅限培训管理员/系统管理员
        List<String> roles = StpUtil.getRoleList();
        if (!roles.contains(ROLE_TRAINING_ADMIN) && !roles.contains(ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅培训管理员或系统管理员可执行免考确认完成");
        }
        // 确认理由必填，保证 GMP 可追溯
        if (StrUtil.isBlank(remark)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "免考确认完成必须填写确认理由");
        }
        TrTrainingTask task = baseMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训任务不存在");
        }
        if (task.getStatus() != null && task.getStatus() == STATUS_COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该任务已完成，请勿重复操作");
        }
        TrTrainingTask update = new TrTrainingTask();
        update.setId(id);
        update.setStatus(STATUS_COMPLETED);
        update.setCompleteTime(LocalDateTime.now());
        // 完成时进度置为 100%
        update.setVideoProgress(new BigDecimal("100"));
        // 免考确认视为已完成两级审核并归档，确认理由记入二级复核意见字段
        update.setReviewStatus(REVIEW_APPROVED);
        update.setQaReviewUserId(StpUtil.getLoginIdAsLong());
        update.setQaReviewTime(LocalDateTime.now());
        update.setQaReviewRemark(remark);
        baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enterReviewChain(Long taskId) {
        TrTrainingTask task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训任务不存在");
        }
        // 置为「待主管审核」；若此前被退回（reviewStatus=3），重新进入审核链同样回到待主管审核
        TrTrainingTask update = new TrTrainingTask();
        update.setId(taskId);
        update.setStatus(STATUS_COMPLETED);
        update.setReviewStatus(REVIEW_PENDING_LEADER);
        update.setCompleteTime(LocalDateTime.now());
        // 视频进度归一为 100%：能进入审核链说明学习与考试均已达标
        update.setVideoProgress(new BigDecimal("100"));
        // 重新进入审核链时清空上一次的审核痕迹，避免歧义
        update.setLeaderReviewUserId(null);
        update.setLeaderReviewTime(null);
        update.setLeaderReviewRemark(null);
        baseMapper.updateById(update);

        // 解析学员所在部门的主管作为一级审核人
        SysUser learner = sysUserMapper.selectById(task.getUserId());
        List<Long> leaderIds = learner == null ? Collections.emptyList()
                : findDeptLeaders(learner.getDepartmentId());
        String taskName = StrUtil.blankToDefault(task.getName(), "培训任务");
        if (leaderIds.isEmpty()) {
            // 部门无主管：回退通知培训管理员/系统管理员兜底
            List<Long> adminIds = findUserIdsByRoleCodes(Arrays.asList(ROLE_TRAINING_ADMIN, ROLE_SUPER_ADMIN));
            for (Long adminId : adminIds) {
                sysMessageService.send(adminId, SysMessageService.TYPE_TASK,
                        "待审核任务缺少部门主管",
                        "任务《" + taskName + "》已达标待审核，但学员所在部门未配置主管，请指定审核人或代为审核。",
                        taskId);
            }
            log.warn("任务 {} 进入审核链但部门无主管，已回退通知管理员，兜底人数={}", taskId, adminIds.size());
        } else {
            for (Long leaderId : leaderIds) {
                sysMessageService.send(leaderId, SysMessageService.TYPE_TASK,
                        "培训记录待审核",
                        "您部门学员的《" + taskName + "》已完成学习与考试，请及时审核。",
                        taskId);
            }
        }
        log.info("任务 {} 进入审核链，一级审核通知人数={}", taskId, Math.max(leaderIds.size(), 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaderReview(Long id, TaskReviewDTO dto) {
        TrTrainingTask task = requireTask(id);
        // 权限校验：一级审核由部门主管执行；培训管理员/系统管理员可兜底
        List<String> roles = StpUtil.getRoleList();
        boolean isAdmin = roles.contains(ROLE_TRAINING_ADMIN) || roles.contains(ROLE_SUPER_ADMIN);
        if (!isAdmin && !roles.contains(ROLE_DEPT_LEADER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅部门主管或培训管理员可执行主管审核");
        }
        // 状态校验：必须处于「待主管审核」
        if (!Objects.equals(task.getReviewStatus(), REVIEW_PENDING_LEADER)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该任务当前不在「待主管审核」阶段");
        }
        // 部门隔离：非管理员的主管只能审核本部门学员的任务
        if (!isAdmin && !sameDepartment(task.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能审核本部门学员的培训任务");
        }
        // 退回必须填写意见
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        if (!pass && StrUtil.isBlank(dto.getRemark())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退回时必须填写退回意见");
        }

        TrTrainingTask update = new TrTrainingTask();
        update.setId(id);
        update.setLeaderReviewUserId(StpUtil.getLoginIdAsLong());
        update.setLeaderReviewTime(LocalDateTime.now());
        update.setLeaderReviewRemark(dto.getRemark());
        String taskName = StrUtil.blankToDefault(task.getName(), "培训任务");
        if (pass) {
            // 通过 → 进入待QA复核，并通知质量管理员
            update.setReviewStatus(REVIEW_PENDING_QA);
            baseMapper.updateById(update);
            for (Long qaId : findUserIdsByRoleCodes(Collections.singletonList(ROLE_QA_MANAGER))) {
                sysMessageService.send(qaId, SysMessageService.TYPE_TASK,
                        "培训记录待复核",
                        "任务《" + taskName + "》已通过主管审核，请进行 QA 复核。",
                        id);
            }
        } else {
            // 退回 → 任务回到待考试，允许学员重新考试
            update.setReviewStatus(REVIEW_REJECTED);
            update.setStatus(STATUS_WAITING_EXAM);
            baseMapper.updateById(update);
            sysMessageService.send(task.getUserId(), SysMessageService.TYPE_TASK,
                    "培训记录被退回",
                    "您的《" + taskName + "》未通过主管审核，请重新学习并考试。退回意见：" + dto.getRemark(),
                    id);
        }
        log.info("主管审核完成：任务ID={}, 结论={}", id, pass ? "通过" : "退回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void qaReview(Long id, TaskReviewDTO dto) {
        TrTrainingTask task = requireTask(id);
        // 权限校验：二级复核仅限质量管理员，系统管理员可兜底
        List<String> roles = StpUtil.getRoleList();
        if (!roles.contains(ROLE_QA_MANAGER) && !roles.contains(ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅质量管理员或系统管理员可执行 QA 复核");
        }
        // 状态校验：必须处于「待QA复核」
        if (!Objects.equals(task.getReviewStatus(), REVIEW_PENDING_QA)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该任务当前不在「待QA复核」阶段");
        }
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        if (!pass && StrUtil.isBlank(dto.getRemark())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退回时必须填写退回意见");
        }

        TrTrainingTask update = new TrTrainingTask();
        update.setId(id);
        update.setQaReviewUserId(StpUtil.getLoginIdAsLong());
        update.setQaReviewTime(LocalDateTime.now());
        update.setQaReviewRemark(dto.getRemark());
        String taskName = StrUtil.blankToDefault(task.getName(), "培训任务");
        if (pass) {
            // 通过 → 归档（任务终态）
            update.setReviewStatus(REVIEW_APPROVED);
            baseMapper.updateById(update);
            sysMessageService.send(task.getUserId(), SysMessageService.TYPE_TASK,
                    "培训已完成归档",
                    "恭喜您，《" + taskName + "》已通过 QA 复核并完成归档。",
                    id);
        } else {
            // 退回 → 任务回到待考试，允许学员重新考试
            update.setReviewStatus(REVIEW_REJECTED);
            update.setStatus(STATUS_WAITING_EXAM);
            baseMapper.updateById(update);
            sysMessageService.send(task.getUserId(), SysMessageService.TYPE_TASK,
                    "培训记录被退回",
                    "您的《" + taskName + "》未通过 QA 复核，请重新学习并考试。退回意见：" + dto.getRemark(),
                    id);
        }
        log.info("QA 复核完成：任务ID={}, 结论={}", id, pass ? "通过" : "退回");
    }

    @Override
    public Page<TaskVO> pendingReviews(Integer pageNum, Integer pageSize, String stage) {
        // 依据传入阶段或当前角色，决定查询的审核状态
        Integer targetReviewStatus = resolvePendingStage(stage);
        Page<TrTrainingTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrTrainingTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrTrainingTask::getReviewStatus, targetReviewStatus);
        // 主管审核阶段且调用者仅为部门负责人时，限制本部门数据
        if (targetReviewStatus == REVIEW_PENDING_LEADER && isDepartmentScoped()) {
            List<Long> deptUserIds = findUserIdsByDepartment(currentUser().getDepartmentId());
            if (deptUserIds.isEmpty()) {
                wrapper.eq(TrTrainingTask::getUserId, -1L);
            } else {
                wrapper.in(TrTrainingTask::getUserId, deptUserIds);
            }
        }
        // 待办按截止时间升序，最紧急的排在最前
        wrapper.orderByAsc(TrTrainingTask::getDeadline);

        Page<TrTrainingTask> entityPage = baseMapper.selectPage(page, wrapper);
        Page<TaskVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<TaskVO> voList = entityPage.getRecords().stream().map(this::toBaseVO).collect(Collectors.toList());
        fillNames(entityPage.getRecords(), voList);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Map<String, Long> reviewStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        // 待主管审核：部门负责人只看本部门，管理员看全部
        LambdaQueryWrapper<TrTrainingTask> leaderWrapper = new LambdaQueryWrapper<>();
        leaderWrapper.eq(TrTrainingTask::getReviewStatus, REVIEW_PENDING_LEADER);
        if (isDepartmentScoped()) {
            List<Long> deptUserIds = findUserIdsByDepartment(currentUser().getDepartmentId());
            if (deptUserIds.isEmpty()) {
                leaderWrapper.eq(TrTrainingTask::getUserId, -1L);
            } else {
                leaderWrapper.in(TrTrainingTask::getUserId, deptUserIds);
            }
        }
        stats.put("leader", baseMapper.selectCount(leaderWrapper));
        // 待QA复核：全局统计
        stats.put("qa", baseMapper.selectCount(new LambdaQueryWrapper<TrTrainingTask>()
                .eq(TrTrainingTask::getReviewStatus, REVIEW_PENDING_QA)));
        return stats;
    }

    /**
     * 解析待审核阶段：显式传入优先；否则按当前登录人角色自动判断
     * <p>
     * 质量管理员默认看「待QA复核」；其余角色默认看「待主管审核」。
     */
    private Integer resolvePendingStage(String stage) {
        if (StrUtil.isNotBlank(stage)) {
            if ("qa".equalsIgnoreCase(stage)) {
                return REVIEW_PENDING_QA;
            }
            if ("leader".equalsIgnoreCase(stage)) {
                return REVIEW_PENDING_LEADER;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核阶段不合法");
        }
        // 未显式指定：质量管理员（且非系统管理员）默认进入 QA 复核阶段
        List<String> roles = StpUtil.getRoleList();
        if (roles.contains(ROLE_QA_MANAGER) && !roles.contains(ROLE_SUPER_ADMIN)) {
            return REVIEW_PENDING_QA;
        }
        return REVIEW_PENDING_LEADER;
    }

    /**
     * 查询任务并校验存在
     */
    private TrTrainingTask requireTask(Long id) {
        TrTrainingTask task = baseMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训任务不存在");
        }
        return task;
    }

    /**
     * 当前登录人是否为「仅部门负责人」（不具备任何管理员角色）
     * <p>
     * 用于决定是否启用部门数据隔离：同时具备管理员角色时不隔离。
     */
    private boolean isDepartmentScoped() {
        List<String> roles = StpUtil.getRoleList();
        if (!roles.contains(ROLE_DEPT_LEADER)) {
            return false;
        }
        return !roles.contains(ROLE_SUPER_ADMIN)
                && !roles.contains(ROLE_QA_MANAGER)
                && !roles.contains(ROLE_TRAINING_ADMIN);
    }

    /**
     * 当前登录用户实体
     */
    private SysUser currentUser() {
        return sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
    }

    /**
     * 判断指定学员是否与当前登录人同部门
     */
    private boolean sameDepartment(Long learnerId) {
        SysUser current = currentUser();
        SysUser learner = sysUserMapper.selectById(learnerId);
        if (current == null || learner == null || current.getDepartmentId() == null) {
            return false;
        }
        return Objects.equals(current.getDepartmentId(), learner.getDepartmentId());
    }

    /**
     * 查询指定部门下的在岗（启用）用户ID列表
     */
    private List<Long> findUserIdsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return Collections.emptyList();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDepartmentId, departmentId)
                        .eq(SysUser::getStatus, USER_STATUS_ENABLED))
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }

    /**
     * 查询指定部门的一级审核人（部门主管）用户ID列表
     * <p>
     * 依据 sys_user.is_department_leader = 1 定位；无主管时返回空列表，由调用方兜底。
     */
    private List<Long> findDeptLeaders(Long departmentId) {
        if (departmentId == null) {
            return Collections.emptyList();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDepartmentId, departmentId)
                        .eq(SysUser::getIsDepartmentLeader, 1)
                        .eq(SysUser::getStatus, USER_STATUS_ENABLED))
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }

    /**
     * 按角色编码查询启用用户ID列表（用于待办角标与兜底通知）
     */
    private List<Long> findUserIdsByRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 角色编码 → 角色ID
        List<Long> roleIds = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getCode, roleCodes)
                        .eq(SysRole::getStatus, 1))
                .stream().map(SysRole::getId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 角色ID → 用户ID
        List<Long> userIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, roleIds))
                .stream().map(SysUserRole::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 过滤出启用状态的用户
        return sysUserMapper.selectBatchIds(userIds).stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == USER_STATUS_ENABLED)
                .map(SysUser::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyTask(Long id) {
        TrTrainingTask task = baseMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训任务不存在");
        }
        // 催办以站内信形式下发给执行人（消息类型：到期提醒）
        sysMessageService.send(task.getUserId(), SysMessageService.TYPE_EXPIRE,
                "培训任务催办提醒",
                "您有未完成的培训任务《" + task.getName() + "》，请尽快完成学习与考试。",
                task.getId());
        log.info("催办通知已发送：任务ID={}, 任务名称={}, 执行人ID={}", task.getId(), task.getName(), task.getUserId());
    }

    /**
     * 实体转基础 VO（含状态枚举与进度计算）
     */
    private TaskVO toBaseVO(TrTrainingTask task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setName(task.getName());
        vo.setDeadline(task.getDeadline() == null ? null : task.getDeadline().toLocalDate());
        vo.setStatus(statusToString(task.getStatus()));
        vo.setProgress(calcProgress(task));
        vo.setDescription(task.getDescription());
        // 审核相关字段回填，供管理端「待我审核」页签与审核弹窗使用
        vo.setReviewStatus(task.getReviewStatus());
        vo.setReviewStatusText(reviewStatusText(task.getReviewStatus()));
        vo.setLeaderReviewRemark(task.getLeaderReviewRemark());
        vo.setQaReviewRemark(task.getQaReviewRemark());
        return vo;
    }

    /**
     * 审核状态数字转中文文案（用于列表直接展示，避免前端重复维护字典）
     */
    private String reviewStatusText(Integer reviewStatus) {
        if (reviewStatus == null) {
            return null;
        }
        switch (reviewStatus) {
            case REVIEW_PENDING_LEADER:
                return "待主管审核";
            case REVIEW_PENDING_QA:
                return "待QA复核";
            case REVIEW_APPROVED:
                return "已复核通过";
            case REVIEW_REJECTED:
                return "已退回";
            default:
                return null;
        }
    }

    /**
     * 计算学习进度百分比
     * <p>
     * 已完成任务固定 100%；否则取 video_progress（视频学习进度）。
     */
    private Integer calcProgress(TrTrainingTask task) {
        if (task.getStatus() != null && task.getStatus() == STATUS_COMPLETED) {
            return 100;
        }
        if (task.getVideoProgress() != null) {
            return task.getVideoProgress().intValue();
        }
        return 0;
    }

    /**
     * 批量回填计划名称、课程名称、执行人姓名与部门名称，避免 N+1 查询
     */
    private void fillNames(List<TrTrainingTask> tasks, List<TaskVO> vos) {
        if (tasks.isEmpty()) {
            return;
        }
        // 批量查询课程名称
        Set<Long> courseIds = tasks.stream().map(TrTrainingTask::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> courseNameMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseNameMap = trCourseMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(TrCourse::getId, TrCourse::getName, (a, b) -> a));
        }
        // 批量查询计划名称
        Set<Long> planIds = tasks.stream().map(TrTrainingTask::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> planNameMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            planNameMap = trTrainingPlanMapper.selectBatchIds(planIds).stream()
                    .collect(Collectors.toMap(TrTrainingPlan::getId, TrTrainingPlan::getName, (a, b) -> a));
        }
        // 批量查询执行人及部门
        Set<Long> userIds = tasks.stream().map(TrTrainingTask::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
            Set<Long> deptIds = users.stream().map(SysUser::getDepartmentId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                deptNameMap = sysDepartmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
            }
        }

        // 按顺序回填
        for (int i = 0; i < tasks.size(); i++) {
            TrTrainingTask task = tasks.get(i);
            TaskVO vo = vos.get(i);
            vo.setCourseName(courseNameMap.get(task.getCourseId()));
            vo.setPlanName(planNameMap.get(task.getPlanId()));
            SysUser user = userMap.get(task.getUserId());
            if (user != null) {
                vo.setAssigneeName(user.getRealName());
                if (user.getDepartmentId() != null) {
                    vo.setDepartmentName(deptNameMap.get(user.getDepartmentId()));
                }
            }
        }
    }

    /**
     * 状态数字转前端字符串
     */
    private String statusToString(Integer status) {
        if (status == null) {
            return "not_started";
        }
        switch (status) {
            case 1:
            case 2:
                return "in_progress";
            case STATUS_COMPLETED:
                return "completed";
            case 4:
                return "overdue";
            default:
                return "not_started";
        }
    }

    /**
     * 前端状态字符串转状态数字集合（空返回 null 表示不过滤）
     */
    private List<Integer> parseStatusList(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        switch (status) {
            case "not_started":
                return Collections.singletonList(STATUS_NOT_STARTED);
            case "in_progress":
                return java.util.Arrays.asList(1, 2);
            case "completed":
                return Collections.singletonList(STATUS_COMPLETED);
            case "overdue":
                return Collections.singletonList(4);
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "任务状态不合法");
        }
    }
}
