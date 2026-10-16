package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.task.TaskAssignDTO;
import com.gmp.dto.task.TaskReviewDTO;
import com.gmp.dto.task.TaskVO;
import com.gmp.entity.TrTrainingTask;

import java.util.Map;

/**
 * 培训任务 Service 接口
 * <p>
 * 除原有的分配/完成/催办外，本接口承载「两级审核闭环」：
 * 学员学习考试达标 → 部门主管审核 → QA 复核 → 归档。
 */
public interface TrTrainingTaskService extends IService<TrTrainingTask> {

    /**
     * 分页查询培训任务
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param name         任务名称（模糊匹配）
     * @param status       任务状态（前端枚举，空表示不过滤）
     * @param assigneeName 执行人姓名（模糊匹配）
     * @param reviewStatus 审核状态（空表示不过滤）
     * @return 分页结果
     */
    Page<TaskVO> pageTasks(Integer pageNum, Integer pageSize, String name, String status,
                           String assigneeName, Integer reviewStatus);

    /**
     * 分配培训任务（按执行人拆分为多条任务记录）
     *
     * @param dto 分配参数
     */
    void assign(TaskAssignDTO dto);

    /**
     * 标记任务完成（免考确认完成）
     * <p>
     * 仅限培训管理员/系统管理员使用，必须填写确认理由；
     * 执行后直接置为已归档（status=3 且 reviewStatus=2）。
     *
     * @param id     任务ID
     * @param remark 确认理由（必填）
     */
    void complete(Long id, String remark);

    /**
     * 催办通知：对指定任务发送学习提醒
     *
     * @param id 任务ID
     */
    void notifyTask(Long id);

    /**
     * 待我审核列表
     * <p>
     * 按当前登录人角色自动决定查询范围：
     * 部门主管查本部门「待主管审核」；QA 查全部「待QA复核」；管理员两者皆可（可用 stage 指定）。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param stage    阶段：leader-待主管审核 qa-待QA复核（空表示按角色自动判断）
     * @return 分页结果
     */
    Page<TaskVO> pendingReviews(Integer pageNum, Integer pageSize, String stage);

    /**
     * 待审核数量统计（用于菜单/页签角标）
     *
     * @return 其中 key 为 leader / qa
     */
    Map<String, Long> reviewStats();

    /**
     * 进入审核链（学员学习与考试达标后调用）
     * <p>
     * 将任务置为「待主管审核」（reviewStatus=0），解析学员所在部门的主管并推送待审核站内信；
     * 若该部门无主管，则回退通知培训管理员/系统管理员兜底。
     *
     * @param taskId 任务ID
     */
    void enterReviewChain(Long taskId);

    /**
     * 一级审核（部门主管审核）
     *
     * @param id  任务ID
     * @param dto 审核参数（pass 为 false 时 remark 必填）
     */
    void leaderReview(Long id, TaskReviewDTO dto);

    /**
     * 二级复核（QA 复核）
     *
     * @param id  任务ID
     * @param dto 审核参数（pass 为 false 时 remark 必填）
     */
    void qaReview(Long id, TaskReviewDTO dto);
}