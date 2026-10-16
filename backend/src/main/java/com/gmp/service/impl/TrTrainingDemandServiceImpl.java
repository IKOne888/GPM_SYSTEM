package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.demand.DemandHandleDTO;
import com.gmp.dto.demand.DemandSaveDTO;
import com.gmp.dto.demand.DemandSubmitDTO;
import com.gmp.dto.demand.DemandVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysUser;
import com.gmp.entity.TrTrainingDemand;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.TrTrainingDemandMapper;
import com.gmp.service.SysMessageService;
import com.gmp.service.TrTrainingDemandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 培训需求 Service 实现类
 * <p>
 * 负责需求台账的查询与状态流转（待处理 → 已采纳 / 已拒绝），
 * 并统一完成「申请部门名称」「状态中文描述」的展示字段回填。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrTrainingDemandServiceImpl extends ServiceImpl<TrTrainingDemandMapper, TrTrainingDemand>
        implements TrTrainingDemandService {

    /** 部门 Mapper（用于回填申请部门名称） */
    private final SysDepartmentMapper sysDepartmentMapper;

    /** 用户 Mapper（学员端提交时取当前登录用户的所属部门） */
    private final SysUserMapper sysUserMapper;

    /** 站内信 Service（需求处理完成后向提交学员回发结果通知） */
    private final SysMessageService sysMessageService;

    /** 状态常量：待处理 */
    private static final int STATUS_PENDING = 0;
    /** 状态常量：已采纳 */
    private static final int STATUS_ACCEPTED = 1;
    /** 状态常量：已拒绝 */
    private static final int STATUS_REJECTED = 2;

    @Override
    public Page<DemandVO> pageDemands(Integer pageNum, Integer pageSize, String keyword, Integer status, Long departmentId) {
        // 构建分页对象
        Page<TrTrainingDemand> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrTrainingDemand> wrapper = new LambdaQueryWrapper<>();
        // 标题关键字模糊查询
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(TrTrainingDemand::getTitle, keyword);
        }
        // 状态精确过滤
        if (status != null) {
            wrapper.eq(TrTrainingDemand::getStatus, status);
        }
        // 申请部门精确过滤
        if (departmentId != null) {
            wrapper.eq(TrTrainingDemand::getDepartmentId, departmentId);
        }
        // 最新登记的排在最前
        wrapper.orderByDesc(TrTrainingDemand::getCreateTime);

        // 执行分页查询（逻辑删除由 MyBatis-Plus 自动过滤）
        Page<TrTrainingDemand> entityPage = baseMapper.selectPage(page, wrapper);

        // 转换为展示 VO
        List<DemandVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        // 批量回填申请部门名称，避免逐条查询造成 N+1
        fillDepartmentName(entityPage.getRecords(), voList);

        // 组装返回分页对象（保留总条数与分页参数）
        Page<DemandVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public DemandVO getDemandDetail(Long id) {
        TrTrainingDemand demand = baseMapper.selectById(id);
        if (demand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训需求不存在");
        }
        DemandVO vo = toVO(demand);
        // 单条详情直接回填部门名称
        fillDepartmentName(java.util.Collections.singletonList(demand), java.util.Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDemand(DemandSaveDTO dto) {
        // 校验申请部门存在性（部门为空时允许登记为「未指定部门」）
        checkDepartmentExists(dto.getDepartmentId());

        TrTrainingDemand demand = buildEntity(dto);
        // 新增需求一律为「待处理」状态
        demand.setStatus(STATUS_PENDING);
        baseMapper.insert(demand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDemand(DemandSaveDTO dto) {
        TrTrainingDemand exist = baseMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训需求不存在");
        }
        // 已处理（采纳/拒绝）的需求不允许再编辑，保证处理结果的严肃性
        if (exist.getStatus() != null && exist.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已处理的需求不允许编辑");
        }
        // 校验申请部门存在性
        checkDepartmentExists(dto.getDepartmentId());

        TrTrainingDemand update = buildEntity(dto);
        // 以路径 ID 为准
        update.setId(dto.getId());
        baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleDemand(Long id, DemandHandleDTO dto) {
        TrTrainingDemand demand = baseMapper.selectById(id);
        if (demand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训需求不存在");
        }
        // 仅「待处理」状态可被处理，防止重复处理导致状态被覆盖
        if (demand.getStatus() == null || demand.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该需求已处理，请勿重复操作");
        }
        Integer target = dto.getStatus();
        // 处理结果只允许「采纳(1)」或「拒绝(2)」
        if (target == null || (target != STATUS_ACCEPTED && target != STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理结果不合法");
        }
        // 拒绝必须说明原因，便于提出部门得到明确反馈
        if (target == STATUS_REJECTED && StrUtil.isBlank(dto.getOpinion())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写拒绝原因");
        }

        TrTrainingDemand update = new TrTrainingDemand();
        update.setId(id);
        update.setStatus(target);
        // 处理意见复用 reason 字段：拒绝时存拒绝原因，采纳时存采纳备注
        if (StrUtil.isNotBlank(dto.getOpinion())) {
            update.setReason(dto.getOpinion());
        }
        baseMapper.updateById(update);

        // 处理完成后，若该需求由学员本人提交（submit_user_id 非空），回发站内信形成闭环；
        // 管理端代录的需求（submit_user_id 为空）跳过通知，保持原行为不变
        notifySubmitter(demand, target, dto.getOpinion());

        log.info("培训需求处理完成：需求ID={}, 标题={}, 处理结果={}", id, demand.getTitle(), target);
    }

    /**
     * 需求处理结果通知：向提交学员回发站内信
     *
     * @param demand  需求实体（含提交人ID与标题）
     * @param target  处理结果：1-已采纳 2-已拒绝
     * @param opinion 处理意见（采纳备注 / 拒绝原因，可为空）
     */
    private void notifySubmitter(TrTrainingDemand demand, Integer target, String opinion) {
        Long submitUserId = demand.getSubmitUserId();
        // 非学员端提交（管理端代录）无需通知
        if (submitUserId == null) {
            return;
        }
        // 结论中文描述
        String conclusion = target == STATUS_ACCEPTED ? "已采纳" : "已拒绝";
        // 拼接结果通知内容：含标题、结论与处理意见
        String content = "您提交的培训需求《" + demand.getTitle() + "》" + conclusion
                + (StrUtil.isNotBlank(opinion) ? "：" + opinion : "。");
        // 以系统通知类型推送，关联需求ID便于前端跳转
        sysMessageService.send(submitUserId, SysMessageService.TYPE_SYSTEM, "培训需求处理结果", content, demand.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDemand(Long id) {
        TrTrainingDemand demand = baseMapper.selectById(id);
        if (demand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训需求不存在");
        }
        // 已处理的需求保留台账，不允许删除
        if (demand.getStatus() != null && demand.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已处理的需求不允许删除");
        }
        // 逻辑删除，数据物理保留以满足审计要求
        baseMapper.deleteById(id);
    }

    // ==================== 学员端（用户端）培训需求申报 ====================

    @Override
    public Page<DemandVO> pageMyDemands(Integer pageNum, Integer pageSize, Integer status) {
        // 取当前登录用户ID作为数据边界，强制只查本人提交的需求
        Long userId = StpUtil.getLoginIdAsLong();
        Page<TrTrainingDemand> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrTrainingDemand> wrapper = new LambdaQueryWrapper<>();
        // 仅本人提交（管理端代录 submit_user_id 为 NULL，天然被排除）
        wrapper.eq(TrTrainingDemand::getSubmitUserId, userId);
        // 状态精确过滤（选填）
        if (status != null) {
            wrapper.eq(TrTrainingDemand::getStatus, status);
        }
        // 最新提交排在最前
        wrapper.orderByDesc(TrTrainingDemand::getCreateTime);

        Page<TrTrainingDemand> entityPage = baseMapper.selectPage(page, wrapper);
        List<DemandVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        // 批量回填申请部门名称，避免 N+1
        fillDepartmentName(entityPage.getRecords(), voList);
        Page<DemandVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public DemandVO getMyDemandDetail(Long id) {
        TrTrainingDemand demand = baseMapper.selectById(id);
        if (demand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "培训需求不存在");
        }
        // 越权校验：非本人提交的需求禁止查看，返回 403
        Long userId = StpUtil.getLoginIdAsLong();
        if (demand.getSubmitUserId() == null || !demand.getSubmitUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人的培训需求");
        }
        DemandVO vo = toVO(demand);
        fillDepartmentName(java.util.Collections.singletonList(demand), java.util.Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitDemand(DemandSubmitDTO dto) {
        // 取当前登录用户，用于确定提交人与申请部门（不信任客户端传值）
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        TrTrainingDemand demand = new TrTrainingDemand();
        demand.setTitle(dto.getTitle());
        demand.setContent(dto.getContent());
        demand.setTrainingTopic(dto.getTrainingTopic());
        demand.setTrainingTarget(dto.getTrainingTarget());
        // 申请部门取当前登录用户所属部门，防止冒用他人部门
        demand.setDepartmentId(user.getDepartmentId());
        // 提交人ID用于「我的申报」隔离与结果通知
        demand.setSubmitUserId(userId);
        // 状态固定为「待处理」，忽略任何客户端传值，防止学员自封结论
        demand.setStatus(STATUS_PENDING);
        baseMapper.insert(demand);
        log.info("学员提交培训需求：需求ID={}, 提交人={}, 标题={}", demand.getId(), userId, demand.getTitle());
        return demand.getId();
    }

    /**
     * DTO 转实体
     *
     * @param dto 保存参数
     * @return 需求实体
     */
    private TrTrainingDemand buildEntity(DemandSaveDTO dto) {
        TrTrainingDemand demand = new TrTrainingDemand();
        demand.setTitle(dto.getTitle());
        demand.setContent(dto.getContent());
        demand.setTrainingTopic(dto.getTrainingTopic());
        demand.setTrainingTarget(dto.getTrainingTarget());
        demand.setReason(dto.getReason());
        demand.setDepartmentId(dto.getDepartmentId());
        return demand;
    }

    /**
     * 实体转展示 VO（补充状态中文描述）
     *
     * @param demand 需求实体
     * @return 需求展示对象
     */
    private DemandVO toVO(TrTrainingDemand demand) {
        DemandVO vo = new DemandVO();
        vo.setId(demand.getId());
        vo.setTitle(demand.getTitle());
        vo.setContent(demand.getContent());
        vo.setTrainingTopic(demand.getTrainingTopic());
        vo.setTrainingTarget(demand.getTrainingTarget());
        vo.setReason(demand.getReason());
        vo.setDepartmentId(demand.getDepartmentId());
        vo.setStatus(demand.getStatus());
        vo.setStatusText(statusToText(demand.getStatus()));
        vo.setCreateTime(demand.getCreateTime());
        vo.setCreateBy(demand.getCreateBy());
        return vo;
    }

    /**
     * 批量回填「申请部门名称」
     *
     * @param demands 需求实体列表
     * @param vos     对应的 VO 列表（顺序与实体列表一致）
     */
    private void fillDepartmentName(List<TrTrainingDemand> demands, List<DemandVO> vos) {
        // 收集非空的部门ID，去重后一次性查询，避免 N+1
        Set<Long> deptIds = demands.stream()
                .map(TrTrainingDemand::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<SysDepartment> depts = sysDepartmentMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
        }
        // 按顺序回填
        for (int i = 0; i < demands.size(); i++) {
            Long deptId = demands.get(i).getDepartmentId();
            if (deptId != null) {
                vos.get(i).setDepartmentName(deptNameMap.get(deptId));
            }
        }
    }

    /**
     * 校验申请部门存在性（为空时跳过校验）
     *
     * @param departmentId 部门ID
     */
    private void checkDepartmentExists(Long departmentId) {
        if (departmentId == null) {
            return;
        }
        if (sysDepartmentMapper.selectById(departmentId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请部门不存在");
        }
    }

    /**
     * 状态数字转中文描述
     *
     * @param status 状态值
     * @return 中文描述
     */
    private String statusToText(Integer status) {
        if (status == null) {
            return "待处理";
        }
        switch (status) {
            case STATUS_ACCEPTED:
                return "已采纳";
            case STATUS_REJECTED:
                return "已拒绝";
            default:
                return "待处理";
        }
    }
}
