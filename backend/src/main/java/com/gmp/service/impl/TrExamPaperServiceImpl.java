package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.RoleLevel;
import com.gmp.dto.exam.PaperRuleDTO;
import com.gmp.dto.exam.PaperSaveDTO;
import com.gmp.dto.exam.PaperVO;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysUser;
import com.gmp.entity.SysUserRole;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrExamPaper;
import com.gmp.entity.TrTrainingTask;
import com.gmp.mapper.SysRoleMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.SysUserRoleMapper;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.TrExamPaperMapper;
import com.gmp.mapper.TrTrainingTaskMapper;
import com.gmp.service.SysMessageService;
import com.gmp.service.TrExamPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 试卷Service实现类
 * <p>
 * 负责试卷前端字符串状态（draft/published/archived）与数据库数字状态（0/1/2）的转换，
 * 以及组卷规则 JSON 的序列化/反序列化。
 * <p>
 * 试卷"发布"时会触发学员端下发流程：通知该课程下已有培训任务的学员，
 * 并为该课程适用岗位的普通员工自动生成培训任务（详见 publishToStudents）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrExamPaperServiceImpl extends ServiceImpl<TrExamPaperMapper, TrExamPaper> implements TrExamPaperService {

    /** 前端状态字符串 → 数据库状态数字 */
    private static final Map<String, Integer> STATUS_TO_INT = new HashMap<>();
    /** 数据库状态数字 → 前端状态字符串 */
    private static final Map<Integer, String> INT_TO_STATUS = new HashMap<>();

    /** 试卷状态：已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 任务来源：系统分配 */
    private static final int TASK_SOURCE_SYSTEM = 1;
    /** 任务状态：待学习 */
    private static final int TASK_STATUS_NOT_STARTED = 0;
    /** 任务类型：正常培训 */
    private static final int TASK_TYPE_NORMAL = 1;
    /** 考试通过标记：已通过 */
    private static final int EXAM_PASS_YES = 1;
    /** 用户状态：启用 */
    private static final int USER_STATUS_ENABLED = 1;
    /** 自动生成培训任务的默认学习周期（天） */
    private static final int DEFAULT_DEADLINE_DAYS = 30;

    /** 课程 Mapper（校验并读取试卷绑定课程） */
    private final TrCourseMapper trCourseMapper;
    /** 培训任务 Mapper（判定学员已有任务 / 已通过考试，并自动生成任务） */
    private final TrTrainingTaskMapper trTrainingTaskMapper;
    /** 用户 Mapper（筛选课程适用岗位的启用员工） */
    private final SysUserMapper sysUserMapper;
    /** 角色 Mapper（定位"普通员工"角色，避免给管理员生成培训任务） */
    private final SysRoleMapper sysRoleMapper;
    /** 用户角色 Mapper */
    private final SysUserRoleMapper sysUserRoleMapper;
    /** 站内信 Service（统一消息写入入口） */
    private final SysMessageService sysMessageService;

    static {
        STATUS_TO_INT.put("draft", 0);
        STATUS_TO_INT.put("published", 1);
        STATUS_TO_INT.put("archived", 2);
        INT_TO_STATUS.put(0, "draft");
        INT_TO_STATUS.put(1, "published");
        INT_TO_STATUS.put(2, "archived");
    }

    @Override
    public Page<PaperVO> pagePapers(Integer pageNum, Integer pageSize, String name, String status) {
        Page<TrExamPaper> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrExamPaper> wrapper = new LambdaQueryWrapper<>();
        // 试卷名称模糊搜索
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(TrExamPaper::getName, name);
        }
        // 状态过滤（字符串 → 数字）
        if (StrUtil.isNotBlank(status)) {
            Integer statusInt = STATUS_TO_INT.get(status);
            if (statusInt == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "试卷状态不合法");
            }
            wrapper.eq(TrExamPaper::getStatus, statusInt);
        }
        wrapper.orderByDesc(TrExamPaper::getCreateTime);

        Page<TrExamPaper> entityPage = baseMapper.selectPage(page, wrapper);
        Page<PaperVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPaper(PaperSaveDTO dto) {
        // 校验试卷编号唯一
        checkCodeUnique(dto.getCode(), null);
        TrExamPaper paper = buildEntity(dto);
        baseMapper.insert(paper);
        // 新增即为"已发布"时，立即向学员端下发考试通知并生成任务
        if (paper.getStatus() != null && paper.getStatus() == STATUS_PUBLISHED) {
            publishToStudents(paper);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(PaperSaveDTO dto) {
        // 校验试卷存在，并保留原状态用于判断是否为"首次发布"
        TrExamPaper exist = baseMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        // 校验编号唯一（排除自身）
        checkCodeUnique(dto.getCode(), dto.getId());
        TrExamPaper paper = buildEntity(dto);
        paper.setId(dto.getId());
        baseMapper.updateById(paper);

        // 触发学员端下发的两种情形（其他情况仅改字段，不重复下发）：
        // 1) 状态首次由非发布变为发布；
        // 2) 试卷本就是已发布，但绑定的课程发生了变化（含由"未绑定"补绑课程）——
        //    此前试卷 course_id 为空，学员端按课程匹配不到它，必须补发通知并补建任务。
        boolean firstPublish = exist.getStatus() == null || exist.getStatus() != STATUS_PUBLISHED;
        boolean courseChanged = !Objects.equals(exist.getCourseId(), paper.getCourseId());
        if (paper.getStatus() != null && paper.getStatus() == STATUS_PUBLISHED
                && (firstPublish || courseChanged)) {
            publishToStudents(paper);
        }
    }

    /**
     * 试卷发布后的学员端下发流程
     * <p>
     * 1) 通知该课程下已有培训任务、且尚未通过考试的学员；
     * 2) 为该课程适用岗位、当前还没有任务的普通员工自动生成培训任务并通知。
     *
     * @param paper 已发布的试卷（courseId 必填）
     */
    private void publishToStudents(TrExamPaper paper) {
        // 试卷发布必须绑定课程，学员端依赖 courseId 匹配试卷
        TrCourse course = trCourseMapper.selectById(paper.getCourseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试卷绑定的培训课程不存在");
        }
        // 该课程下已有的全部任务记录
        List<TrTrainingTask> existTasks = trTrainingTaskMapper.selectList(
                new LambdaQueryWrapper<TrTrainingTask>()
                        .eq(TrTrainingTask::getCourseId, course.getId()));

        // 已通过考试的学员：既不重复通知，也不重复生成任务
        Set<Long> passedUserIds = existTasks.stream()
                .filter(t -> t.getExamPass() != null && t.getExamPass() == EXAM_PASS_YES)
                .map(TrTrainingTask::getUserId)
                .collect(Collectors.toSet());

        // 1. 已有任务的学员：按用户取最新一条任务作为关联ID，补发考试发布通知
        Map<Long, TrTrainingTask> latestTaskMap = new HashMap<>();
        for (TrTrainingTask task : existTasks) {
            latestTaskMap.merge(task.getUserId(), task, (a, b) -> a.getId() >= b.getId() ? a : b);
        }
        for (TrTrainingTask task : latestTaskMap.values()) {
            if (passedUserIds.contains(task.getUserId())) {
                continue;
            }
            sysMessageService.send(task.getUserId(), SysMessageService.TYPE_TASK,
                    "新考试发布通知",
                    "《" + course.getName() + "》已发布考试《" + paper.getName()
                            + "》，请完成课程学习后及时参加考试。",
                    task.getId());
        }

        // 2. 尚未分配任务的适用岗位员工：自动生成培训任务并通知
        createTasksForEligibleUsers(course, paper, existTasks, passedUserIds);
    }

    /**
     * 为课程适用岗位的普通员工自动生成培训任务，并推送站内信
     *
     * @param course        试卷绑定的课程
     * @param paper         已发布的试卷
     * @param existTasks    该课程已有任务（用于排除已分配学员）
     * @param passedUserIds 已通过考试的学员ID集合（跳过，避免重复培训）
     */
    private void createTasksForEligibleUsers(TrCourse course, TrExamPaper paper,
                                             List<TrTrainingTask> existTasks, Set<Long> passedUserIds) {
        // 已分配过任务的学员不再重复生成
        Set<Long> assignedUserIds = existTasks.stream()
                .map(TrTrainingTask::getUserId)
                .collect(Collectors.toSet());

        // 仅面向"普通员工"角色下发，避免给管理员生成培训任务
        SysRole employeeRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, RoleLevel.EMPLOYEE)
                .last("limit 1"));
        if (employeeRole == null) {
            log.warn("未找到普通员工角色，跳过自动生成培训任务：courseId={}", course.getId());
            return;
        }
        Set<Long> employeeUserIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, employeeRole.getId())).stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toSet());
        if (employeeUserIds.isEmpty()) {
            return;
        }

        // 筛选启用状态的员工；课程未指定适用岗位时视为全岗位通用
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, USER_STATUS_ENABLED)
                .in(SysUser::getId, employeeUserIds);
        if (course.getPositionId() != null) {
            userWrapper.eq(SysUser::getPositionId, course.getPositionId());
        }
        List<SysUser> candidates = sysUserMapper.selectList(userWrapper);

        // 自动生成任务的默认截止时间：发布之日起 30 天
        LocalDateTime deadline = LocalDateTime.now().plusDays(DEFAULT_DEADLINE_DAYS);
        for (SysUser user : candidates) {
            if (assignedUserIds.contains(user.getId()) || passedUserIds.contains(user.getId())) {
                continue;
            }
            TrTrainingTask task = new TrTrainingTask();
            task.setName(course.getName() + " - 在线学习与考试");
            task.setCourseId(course.getId());
            task.setUserId(user.getId());
            task.setTaskType(TASK_TYPE_NORMAL);
            task.setSource(TASK_SOURCE_SYSTEM);
            task.setDescription("由试卷《" + paper.getName() + "》发布自动生成，请先完成课程学习后参加考试。");
            task.setStatus(TASK_STATUS_NOT_STARTED);
            task.setDeadline(deadline);
            task.setVideoProgress(BigDecimal.ZERO);
            task.setRetryCount(0);
            trTrainingTaskMapper.insert(task);

            // 任务生成后立即推送站内信
            sysMessageService.send(user.getId(), SysMessageService.TYPE_TASK,
                    "新培训任务通知",
                    "您有新的培训任务《" + course.getName() + "》，请于 " + deadline.toLocalDate()
                            + " 前完成课程学习并参加考试。",
                    task.getId());
            log.info("试卷发布自动生成培训任务：paperId={}, userId={}, taskId={}",
                    paper.getId(), user.getId(), task.getId());
        }
    }

    /**
     * 将保存 DTO 转换为实体（含状态映射、规则 JSON、题目数量计算）
     */
    private TrExamPaper buildEntity(PaperSaveDTO dto) {
        // 状态映射，未传默认草稿
        Integer statusInt = dto.getStatus() == null ? 0 : STATUS_TO_INT.get(dto.getStatus());
        if (statusInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "试卷状态不合法");
        }
        // 及格分不得大于总分
        if (dto.getPassScore() != null && dto.getTotalScore() != null
                && dto.getPassScore() > dto.getTotalScore()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "及格分不能大于总分");
        }
        // 发布状态下必须绑定课程：学员端按「课程ID + 已发布」匹配试卷，未绑定则学员无法进入考试
        if (statusInt == STATUS_PUBLISHED && dto.getCourseId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "试卷发布前必须绑定培训课程");
        }

        TrExamPaper paper = new TrExamPaper();
        paper.setCode(dto.getCode());
        paper.setName(dto.getName());
        // 绑定课程：学员端据此按「课程ID + 已发布」匹配试卷
        paper.setCourseId(dto.getCourseId());
        paper.setTotalScore(dto.getTotalScore());
        paper.setPassScore(dto.getPassScore());
        paper.setExamDuration(dto.getDuration());
        paper.setCompositionType(StrUtil.blankToDefault(dto.getCompositionType(), "random"));
        paper.setStatus(statusInt);

        // 随机组卷：规则序列化为 JSON，并按规则数量之和计算题目数
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            paper.setRules(JSONUtil.toJsonStr(dto.getRules()));
            int count = 0;
            for (PaperRuleDTO rule : dto.getRules()) {
                if (rule.getCount() != null) {
                    count += rule.getCount();
                }
            }
            paper.setQuestionCount(count);
        } else {
            paper.setRules(null);
            paper.setQuestionCount(0);
        }
        return paper;
    }

    /**
     * 校验试卷编号唯一（排除指定ID自身）
     */
    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<TrExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrExamPaper::getCode, code);
        if (excludeId != null) {
            wrapper.ne(TrExamPaper::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "试卷编号已存在");
        }
    }

    /**
     * 实体转 VO（状态反向映射 + 规则 JSON 解析）
     */
    private PaperVO toVO(TrExamPaper paper) {
        PaperVO vo = new PaperVO();
        vo.setId(paper.getId());
        vo.setCode(paper.getCode());
        vo.setName(paper.getName());
        vo.setCourseId(paper.getCourseId());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setDuration(paper.getExamDuration());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setCompositionType(paper.getCompositionType());
        vo.setStatus(INT_TO_STATUS.getOrDefault(paper.getStatus(), "draft"));
        vo.setCreateTime(paper.getCreateTime());
        if (StrUtil.isNotBlank(paper.getRules())) {
            vo.setRules(JSONUtil.toList(paper.getRules(), PaperRuleDTO.class));
        } else {
            vo.setRules(new ArrayList<>());
        }
        return vo;
    }
}
