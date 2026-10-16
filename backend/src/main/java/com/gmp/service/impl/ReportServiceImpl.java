package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gmp.dto.report.*;
import com.gmp.entity.*;
import com.gmp.mapper.*;
import com.gmp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计报表Service实现类
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 部门Mapper */
    private final SysDepartmentMapper departmentMapper;

    /** 用户Mapper */
    private final SysUserMapper userMapper;

    /** 培训任务Mapper */
    private final TrTrainingTaskMapper taskMapper;

    /** 课程Mapper */
    private final TrCourseMapper courseMapper;

    /** 课程分类Mapper */
    private final TrCourseCategoryMapper categoryMapper;

    /** 考试记录Mapper */
    private final TrExamRecordMapper examRecordMapper;

    @Override
    public ReportSummaryVO getSummary(String year, Long departmentId) {
        ReportSummaryVO summary = new ReportSummaryVO();

        // 1. 查询全部启用部门
        List<SysDepartment> departments = departmentMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getStatus, 1)
                        .orderByAsc(SysDepartment::getSort));

        // 2. 查询全部用户（用于按部门统计人数、建立用户-部门映射）
        List<SysUser> users = userMapper.selectList(null);
        // 建立 用户ID -> 部门ID 映射
        Map<Long, Long> userDeptMap = users.stream()
                .filter(u -> u.getDepartmentId() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getDepartmentId, (a, b) -> a));

        // 3. 查询培训任务（支持按年度过滤）
        LambdaQueryWrapper<TrTrainingTask> taskWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(year)) {
            // 按任务创建时间所在年份过滤
            taskWrapper.apply("YEAR(create_time) = {0}", Integer.parseInt(year));
        }
        List<TrTrainingTask> tasks = taskMapper.selectList(taskWrapper);

        // 4. 查询全部课程与分类（用于课程分类占比统计）
        List<TrCourse> courses = courseMapper.selectList(null);
        List<TrCourseCategory> categories = categoryMapper.selectList(null);
        // 建立 分类ID -> 分类名称 映射
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(TrCourseCategory::getId, TrCourseCategory::getName, (a, b) -> a));

        // 5. 查询全部考试记录（用于成绩分布统计）
        List<TrExamRecord> examRecords = examRecordMapper.selectList(null);

        // ============ 统计概览指标 ============
        OverviewVO overview = new OverviewVO();
        // 培训总人次 = 任务总数
        overview.setTotalTrainees(tasks.size());
        // 总培训学时 = 学习时长总和（秒）转换为小时
        overview.setTotalHours((int) Math.round(tasks.stream()
                .mapToLong(t -> t.getStudyDuration() == null ? 0L : t.getStudyDuration()).sum() / 3600.0));
        // 考试通过率 = 通过任务数 / 已考试任务数 * 100
        long examCount = tasks.stream().filter(t -> t.getExamScore() != null).count();
        long passCount = tasks.stream().filter(t -> t.getExamPass() != null && t.getExamPass() == 1).count();
        overview.setPassRate(examCount == 0 ? 0.0 : round1(passCount * 100.0 / examCount));
        // 计划完成率 = 已完成任务数(状态3) / 总任务数 * 100
        long completedCount = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 3).count();
        overview.setCompletionRate(tasks.isEmpty() ? 0.0 : round1(completedCount * 100.0 / tasks.size()));
        summary.setOverview(overview);

        // ============ 部门统计明细 ============
        List<DepartmentStatVO> deptStats = new ArrayList<>();
        // 存储每个部门的图表数据（部门名 -> 完成率）
        List<String> deptLabels = new ArrayList<>();
        List<Double> deptValues = new ArrayList<>();

        for (SysDepartment dept : departments) {
            DepartmentStatVO vo = buildDepartmentStat(dept, users, tasks, userDeptMap);
            if (vo == null) {
                continue;
            }
            deptStats.add(vo);
            deptLabels.add(vo.getDepartmentName());
            deptValues.add(vo.getPlanCompletionRate());
        }
        summary.setDepartmentStats(deptStats);

        // 部门完成率条形图数据
        ChartSeriesVO deptCompletion = new ChartSeriesVO();
        deptCompletion.setLabels(deptLabels);
        deptCompletion.setValues(deptValues);
        summary.setDeptCompletion(deptCompletion);

        // ============ 月度培训学时 ============
        summary.setMonthHours(buildMonthHours(tasks));

        // ============ 考试成绩分布 ============
        summary.setScoreDistribution(buildScoreDistribution(examRecords, userDeptMap, departmentId));

        // ============ 课程分类占比 ============
        summary.setCategoryDistribution(buildCategoryDistribution(courses, categoryNameMap));

        return summary;
    }

    @Override
    public List<DepartmentStatExportVO> exportDepartmentStats(String year, Long departmentId) {
        // 复用汇总统计逻辑
        ReportSummaryVO summary = getSummary(year, departmentId);
        // 将部门统计明细转换为导出模型
        return summary.getDepartmentStats().stream().map(s -> {
            DepartmentStatExportVO vo = new DepartmentStatExportVO();
            vo.setDepartmentName(s.getDepartmentName());
            vo.setTotalEmployees(s.getTotalEmployees());
            vo.setTrainedCount(s.getTrainedCount());
            vo.setTrainingRate(s.getTrainingRate());
            vo.setTotalHours(s.getTotalHours());
            vo.setAvgHours(s.getAvgHours());
            vo.setExamPassRate(s.getExamPassRate());
            vo.setPlanCompletionRate(s.getPlanCompletionRate());
            vo.setOverdueTasks(s.getOverdueTasks());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建单个部门的培训统计
     */
    private DepartmentStatVO buildDepartmentStat(SysDepartment dept, List<SysUser> users,
                                                 List<TrTrainingTask> tasks, Map<Long, Long> userDeptMap) {
        DepartmentStatVO vo = new DepartmentStatVO();
        vo.setDepartmentId(dept.getId());
        vo.setDepartmentName(dept.getName());

        // 该部门下的全部用户
        List<SysUser> deptUsers = users.stream()
                .filter(u -> dept.getId().equals(u.getDepartmentId()))
                .collect(Collectors.toList());
        // 部门总人数
        vo.setTotalEmployees(deptUsers.size());

        // 收集该部门用户的ID集合
        Set<Long> deptUserIds = deptUsers.stream().map(SysUser::getId).collect(Collectors.toSet());

        // 该部门相关的培训任务
        List<TrTrainingTask> deptTasks = tasks.stream()
                .filter(t -> deptUserIds.contains(t.getUserId()))
                .collect(Collectors.toList());

        // 参训人数 = 有任务的去重用户数
        long trainedCount = deptTasks.stream().map(TrTrainingTask::getUserId).distinct().count();
        vo.setTrainedCount((int) trainedCount);

        // 培训覆盖率 = 参训人数 / 总人数 * 100
        vo.setTrainingRate(deptUsers.isEmpty() ? 0.0 : round1(trainedCount * 100.0 / deptUsers.size()));

        // 总学时 = 学习时长总和（秒）转换为小时
        long totalSeconds = deptTasks.stream()
                .mapToLong(t -> t.getStudyDuration() == null ? 0L : t.getStudyDuration()).sum();
        vo.setTotalHours((int) Math.round(totalSeconds / 3600.0));

        // 人均学时
        vo.setAvgHours(trainedCount == 0 ? 0.0 : round1(totalSeconds / 3600.0 / trainedCount));

        // 考试通过率 = 通过任务数 / 已考试任务数 * 100
        long deptExamCount = deptTasks.stream().filter(t -> t.getExamScore() != null).count();
        long deptPassCount = deptTasks.stream().filter(t -> t.getExamPass() != null && t.getExamPass() == 1).count();
        vo.setExamPassRate(deptExamCount == 0 ? 0.0 : round1(deptPassCount * 100.0 / deptExamCount));

        // 计划完成率 = 已完成任务数(状态3) / 总任务数 * 100
        long deptCompleted = deptTasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 3).count();
        vo.setPlanCompletionRate(deptTasks.isEmpty() ? 0.0 : round1(deptCompleted * 100.0 / deptTasks.size()));

        // 逾期任务数（状态4-已逾期）
        vo.setOverdueTasks((int) deptTasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 4).count());

        return vo;
    }

    /**
     * 构建月度培训学时数据（1-12月）
     */
    private ChartSeriesVO buildMonthHours(List<TrTrainingTask> tasks) {
        ChartSeriesVO vo = new ChartSeriesVO();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            labels.add(month + "月");
            final int m = month;
            // 统计指定月份的学习时长（秒）并转换为小时
            double hours = tasks.stream()
                    .filter(t -> t.getCreateTime() != null && t.getCreateTime().getMonthValue() == m)
                    .mapToLong(t -> t.getStudyDuration() == null ? 0L : t.getStudyDuration()).sum() / 3600.0;
            values.add(round1(hours));
        }
        vo.setLabels(labels);
        vo.setValues(values);
        return vo;
    }

    /**
     * 构建考试成绩分布数据（分4个分数段）
     */
    private List<NameValueVO> buildScoreDistribution(List<TrExamRecord> records,
                                                     Map<Long, Long> userDeptMap, Long departmentId) {
        // 若指定了部门，则只统计该部门员工的考试记录
        List<TrExamRecord> filtered = records;
        if (departmentId != null) {
            filtered = records.stream()
                    .filter(r -> departmentId.equals(userDeptMap.get(r.getUserId())))
                    .collect(Collectors.toList());
        }

        // 只统计已提交（状态>=1）且有成绩的记录
        List<Integer> scores = filtered.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() >= 1 && r.getScore() != null)
                .map(TrExamRecord::getScore)
                .collect(Collectors.toList());

        // 分档统计：<60、60-79、80-89、90-100
        List<NameValueVO> result = new ArrayList<>();
        result.add(new NameValueVO("60分以下", countByRange(scores, 0, 59)));
        result.add(new NameValueVO("60-79分", countByRange(scores, 60, 79)));
        result.add(new NameValueVO("80-89分", countByRange(scores, 80, 89)));
        result.add(new NameValueVO("90-100分", countByRange(scores, 90, 100)));
        return result;
    }

    /**
     * 构建课程分类占比数据
     */
    private List<NameValueVO> buildCategoryDistribution(List<TrCourse> courses, Map<Long, String> categoryNameMap) {
        // 按分类ID分组统计课程数量
        Map<Long, Long> countByCategory = courses.stream()
                .filter(c -> c.getCategoryId() != null)
                .collect(Collectors.groupingBy(TrCourse::getCategoryId, Collectors.counting()));

        List<NameValueVO> result = new ArrayList<>();
        // 按分类名称排序输出
        countByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> result.add(new NameValueVO(
                        categoryNameMap.getOrDefault(e.getKey(), "未分类"),
                        e.getValue().intValue())));
        return result;
    }

    /**
     * 统计指定分数区间内的成绩个数
     */
    private int countByRange(List<Integer> scores, int min, int max) {
        return (int) scores.stream().filter(s -> s >= min && s <= max).count();
    }

    /**
     * 保留一位小数
     */
    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
