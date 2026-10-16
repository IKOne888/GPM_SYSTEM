package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.plan.PlanListVO;
import com.gmp.dto.plan.PlanSaveDTO;
import com.gmp.dto.plan.PlanVO;
import com.gmp.entity.TrTrainingPlan;

import java.util.List;

/**
 * 培训计划 Service 接口
 */
public interface TrTrainingPlanService extends IService<TrTrainingPlan> {

    /**
     * 分页查询培训计划
     *
     * @param pageNum        页码
     * @param pageSize       每页条数
     * @param name           计划名称（模糊匹配）
     * @param approvalStatus 审批状态（draft/pending/approved/rejected，空表示不过滤）
     * @param year           计划年度（空表示不过滤）
     * @return 分页结果
     */
    Page<PlanVO> pagePlans(Integer pageNum, Integer pageSize, String name, String approvalStatus, Integer year);

    /**
     * 查询全部计划下拉列表（含关联课程用）
     *
     * @return 计划下拉项列表
     */
    List<PlanListVO> listPlans();

    /**
     * 新增培训计划（保存为草稿）
     *
     * @param dto 保存参数
     */
    void addPlan(PlanSaveDTO dto);

    /**
     * 编辑培训计划（仅草稿或已驳回状态可编辑）
     *
     * @param dto 保存参数
     */
    void updatePlan(PlanSaveDTO dto);

    /**
     * 提交审批：草稿/已驳回 → 待审批
     *
     * @param id 计划ID
     */
    void submitApproval(Long id);

    /**
     * 审批通过：待审批 → 已通过
     *
     * @param id      计划ID
     * @param comment 审批意见
     */
    void approve(Long id, String comment);

    /**
     * 驳回：待审批 → 已驳回
     *
     * @param id      计划ID
     * @param comment 驳回原因（必填）
     */
    void reject(Long id, String comment);
}
