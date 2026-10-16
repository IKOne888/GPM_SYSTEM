package com.gmp.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.notice.NoticePublishDTO;
import com.gmp.dto.notice.NoticeTargetVO;
import com.gmp.dto.notice.NoticeVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysPosition;
import com.gmp.entity.SysUser;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysMessageMapper;
import com.gmp.mapper.SysPositionMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.service.NoticeService;
import com.gmp.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知公告 Service 实现类
 * <p>
 * 公告复用 sys_message 承载（message_type = 4 系统通知、related_id 置空以区别于业务通知），
 * 发布时按接收范围解析目标用户并逐条推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    /** 接收范围：全员 */
    private static final int TARGET_ALL = 1;
    /** 接收范围：按部门 */
    private static final int TARGET_DEPARTMENT = 2;
    /** 接收范围：按岗位 */
    private static final int TARGET_POSITION = 3;

    /** 用户 Mapper（解析接收人集合） */
    private final SysUserMapper sysUserMapper;
    /** 消息 Mapper（公告历史聚合查询） */
    private final SysMessageMapper sysMessageMapper;
    /** 部门 Mapper（接收对象下拉） */
    private final SysDepartmentMapper sysDepartmentMapper;
    /** 岗位 Mapper（接收对象下拉） */
    private final SysPositionMapper sysPositionMapper;
    /** 站内信 Service（统一写入入口） */
    private final SysMessageService sysMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publish(NoticePublishDTO dto) {
        Integer targetType = dto.getTargetType();
        // 接收范围不合法直接拒绝
        if (targetType == null
                || (targetType != TARGET_ALL && targetType != TARGET_DEPARTMENT && targetType != TARGET_POSITION)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "接收范围不合法");
        }
        // 非全员时接收对象不得为空
        if (targetType != TARGET_ALL && CollUtil.isEmpty(dto.getTargetIds())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择接收对象");
        }

        // 解析目标用户ID集合（仅启用状态用户）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId).eq(SysUser::getStatus, 1);
        if (targetType == TARGET_DEPARTMENT) {
            wrapper.in(SysUser::getDepartmentId, dto.getTargetIds());
        } else if (targetType == TARGET_POSITION) {
            wrapper.in(SysUser::getPositionId, dto.getTargetIds());
        }
        List<Long> userIds = sysUserMapper.selectList(wrapper).stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());
        // 未匹配到接收人时明确提示，避免"发送成功但无人收到"的错觉
        if (userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未匹配到任何接收人，请检查接收范围");
        }

        // 逐条推送站内信（relatedId 置空以区别于业务通知）
        for (Long userId : userIds) {
            sysMessageService.send(userId, SysMessageService.TYPE_SYSTEM, dto.getTitle(), dto.getContent(), null);
        }
        log.info("通知公告发布完成：标题={}, 接收范围={}, 触达人数={}", dto.getTitle(), targetType, userIds.size());
        return userIds.size();
    }

    @Override
    public Page<NoticeVO> pageNotices(Integer pageNum, Integer pageSize) {
        // 按「标题 + 正文」聚合，统计触达人数并取最早创建时间作为发布时间
        QueryWrapper<com.gmp.entity.SysMessage> wrapper = new QueryWrapper<>();
        wrapper.select("MIN(create_time) AS create_time", "title", "content", "COUNT(*) AS receive_count")
                .eq("message_type", SysMessageService.TYPE_SYSTEM)
                // 仅统计手工公告（related_id 为空），排除需求处理结果等业务通知
                .isNull("related_id")
                .groupBy("title", "content")
                .orderByDesc("create_time");

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        IPage<Map<String, Object>> result = sysMessageMapper.selectMapsPage(page, wrapper);

        List<NoticeVO> voList = result.getRecords().stream().map(this::mapToVO).collect(Collectors.toList());
        Page<NoticeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public NoticeTargetVO targets() {
        NoticeTargetVO vo = new NoticeTargetVO();
        // 启用状态的部门
        List<SysDepartment> departments = sysDepartmentMapper.selectList(new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getStatus, 1)
                .orderByAsc(SysDepartment::getSort));
        vo.setDepartments(departments.stream()
                .map(d -> new NoticeTargetVO.Item(d.getId(), d.getName()))
                .collect(Collectors.toList()));
        // 启用状态的岗位
        List<SysPosition> positions = sysPositionMapper.selectList(new LambdaQueryWrapper<SysPosition>()
                .eq(SysPosition::getStatus, 1)
                .orderByAsc(SysPosition::getId));
        vo.setPositions(positions.stream()
                .map(p -> new NoticeTargetVO.Item(p.getId(), p.getName()))
                .collect(Collectors.toList()));
        return vo;
    }

    /**
     * 聚合查询结果 Map 转展示 VO
     *
     * @param map 聚合结果（create_time / title / content / receive_count）
     * @return 公告历史 VO
     */
    private NoticeVO mapToVO(Map<String, Object> map) {
        NoticeVO vo = new NoticeVO();
        vo.setTitle((String) map.get("title"));
        vo.setContent((String) map.get("content"));
        vo.setCreateTime(toLocalDateTime(map.get("create_time")));
        Object count = map.get("receive_count");
        vo.setReceiveCount(count == null ? 0L : Long.parseLong(count.toString()));
        return vo;
    }

    /**
     * 兼容 JDBC 返回的时间类型，统一转换为 LocalDateTime
     *
     * @param value 数据库时间字段值
     * @return LocalDateTime，无法识别时返回 null
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return null;
    }
}