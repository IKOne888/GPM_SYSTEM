package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.demand.DemandHandleDTO;
import com.gmp.dto.demand.DemandSaveDTO;
import com.gmp.dto.demand.DemandSubmitDTO;
import com.gmp.dto.demand.DemandVO;
import com.gmp.entity.TrTrainingDemand;

/**
 * 培训需求 Service 接口
 * <p>
 * 承载培训需求台账的查询、登记、编辑、处理（采纳/拒绝）与删除能力。
 */
public interface TrTrainingDemandService extends IService<TrTrainingDemand> {

    /**
     * 分页查询培训需求
     *
     * @param pageNum      页码（从1开始）
     * @param pageSize     每页条数
     * @param keyword      标题关键字（模糊匹配，可为空）
     * @param status       状态过滤（可为空）
     * @param departmentId 申请部门过滤（可为空）
     * @return 需求分页数据（含部门名称与状态中文描述）
     */
    Page<DemandVO> pageDemands(Integer pageNum, Integer pageSize, String keyword, Integer status, Long departmentId);

    /**
     * 查询需求详情（编辑回显用）
     *
     * @param id 需求ID
     * @return 需求展示对象
     */
    DemandVO getDemandDetail(Long id);

    /**
     * 新增培训需求
     *
     * @param dto 保存参数
     */
    void addDemand(DemandSaveDTO dto);

    /**
     * 编辑培训需求（仅「待处理」状态允许编辑）
     *
     * @param dto 保存参数（须包含 id）
     */
    void updateDemand(DemandSaveDTO dto);

    /**
     * 处理培训需求：采纳或拒绝（仅「待处理」状态允许处理）
     *
     * @param id  需求ID
     * @param dto 处理参数（status 1-采纳 2-拒绝，拒绝时 opinion 必填）
     */
    void handleDemand(Long id, DemandHandleDTO dto);

    /**
     * 删除培训需求（仅「待处理」状态允许删除，逻辑删除保留留痕）
     *
     * @param id 需求ID
     */
    void deleteDemand(Long id);

    // ==================== 学员端（用户端）培训需求申报 ====================

    /**
     * 分页查询「我的申报」：仅返回当前登录学员本人提交的需求
     * <p>
     * 数据隔离由本方法以当前登录用户为边界强制过滤（submit_user_id = 当前登录用户），
     * 不依赖前端传参；管理端代录（submit_user_id 为空）的记录不会出现。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param status   状态过滤（可为空：0-待处理 1-已采纳 2-已拒绝）
     * @return 需求分页数据
     */
    Page<DemandVO> pageMyDemands(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查询「我的申报」详情：非本人提交返回 403
     *
     * @param id 需求ID
     * @return 需求展示对象
     */
    DemandVO getMyDemandDetail(Long id);

    /**
     * 提交培训需求（学员端）
     * <p>
     * 服务端强制：状态固定为「待处理」、部门取当前登录用户的所属部门、提交人取当前登录用户ID，
     * 均忽略客户端传值，防止学员自封「已采纳」或冒用他人部门。
     *
     * @param dto 提交参数
     * @return 新需求ID
     */
    Long submitDemand(DemandSubmitDTO dto);
}
