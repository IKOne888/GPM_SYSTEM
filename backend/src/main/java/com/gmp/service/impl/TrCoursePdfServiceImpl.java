package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.pdf.CoursePdfVO;
import com.gmp.entity.SysUser;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCoursePdf;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.TrCoursePdfMapper;
import com.gmp.service.TrCoursePdfService;
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
 * PDF 课件 Service 实现类
 * <p>
 * 负责课件分页查询与启用/停用，并统一完成「所属课程名称」「上传人姓名」的批量回填。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrCoursePdfServiceImpl extends ServiceImpl<TrCoursePdfMapper, TrCoursePdf>
        implements TrCoursePdfService {

    /** 课程 Mapper（用于回填课程名称、校验课程存在性） */
    private final TrCourseMapper trCourseMapper;
    /** 用户 Mapper（用于回填上传人姓名） */
    private final SysUserMapper sysUserMapper;

    /** 状态常量：禁用 */
    private static final int STATUS_DISABLED = 0;
    /** 状态常量：启用 */
    private static final int STATUS_ENABLED = 1;

    @Override
    public Page<CoursePdfVO> pagePdfs(Integer pageNum, Integer pageSize, Long courseId, String keyword, Integer status) {
        // 构建分页对象
        Page<TrCoursePdf> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrCoursePdf> wrapper = new LambdaQueryWrapper<>();
        // 所属课程精确过滤
        if (courseId != null) {
            wrapper.eq(TrCoursePdf::getCourseId, courseId);
        }
        // 课件名称模糊查询
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(TrCoursePdf::getName, keyword);
        }
        // 状态精确过滤
        if (status != null) {
            wrapper.eq(TrCoursePdf::getStatus, status);
        }
        // 最近上传的排在最前
        wrapper.orderByDesc(TrCoursePdf::getUploadTime);

        // 执行分页查询（逻辑删除由 MyBatis-Plus 自动过滤）
        Page<TrCoursePdf> entityPage = baseMapper.selectPage(page, wrapper);

        // 转换为展示 VO
        List<CoursePdfVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        // 批量回填课程名称与上传人姓名，避免 N+1
        fillCourseName(entityPage.getRecords(), voList);
        fillUploadUserName(entityPage.getRecords(), voList);

        // 组装返回分页对象
        Page<CoursePdfVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        TrCoursePdf pdf = baseMapper.selectById(id);
        if (pdf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PDF课件不存在");
        }
        // 仅允许设置为 0-禁用 或 1-启用
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不合法");
        }
        TrCoursePdf update = new TrCoursePdf();
        update.setId(id);
        update.setStatus(status);
        baseMapper.updateById(update);
    }

    /**
     * 实体转展示 VO
     *
     * @param pdf 课件实体
     * @return 课件展示对象
     */
    private CoursePdfVO toVO(TrCoursePdf pdf) {
        CoursePdfVO vo = new CoursePdfVO();
        vo.setId(pdf.getId());
        vo.setCourseId(pdf.getCourseId());
        vo.setName(pdf.getName());
        vo.setFileName(pdf.getFileName());
        vo.setFileSize(pdf.getFileSize());
        vo.setUploadUserId(pdf.getUploadUserId());
        vo.setUploadTime(pdf.getUploadTime());
        vo.setStatus(pdf.getStatus());
        return vo;
    }

    /**
     * 批量回填「所属课程名称」
     *
     * @param pdfs 课件实体列表
     * @param vos  对应的 VO 列表（顺序与实体列表一致）
     */
    private void fillCourseName(List<TrCoursePdf> pdfs, List<CoursePdfVO> vos) {
        // 收集课程ID并去重，一次性批量查询
        Set<Long> courseIds = pdfs.stream()
                .map(TrCoursePdf::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return;
        }
        List<TrCourse> courses = trCourseMapper.selectBatchIds(courseIds);
        Map<Long, String> courseNameMap = courses.stream()
                .collect(Collectors.toMap(TrCourse::getId, TrCourse::getName, (a, b) -> a));
        // 按顺序回填
        for (int i = 0; i < pdfs.size(); i++) {
            Long cid = pdfs.get(i).getCourseId();
            if (cid != null) {
                vos.get(i).setCourseName(courseNameMap.get(cid));
            }
        }
    }

    /**
     * 批量回填「上传人姓名」
     *
     * @param pdfs 课件实体列表
     * @param vos  对应的 VO 列表（顺序与实体列表一致）
     */
    private void fillUploadUserName(List<TrCoursePdf> pdfs, List<CoursePdfVO> vos) {
        // 收集上传人ID并去重，一次性批量查询
        Set<Long> userIds = pdfs.stream()
                .map(TrCoursePdf::getUploadUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, String> userNameMap = new HashMap<>();
        for (SysUser user : users) {
            // 优先展示真实姓名，缺失时回退为登录账号
            userNameMap.put(user.getId(), StrUtil.isNotBlank(user.getRealName()) ? user.getRealName() : user.getUsername());
        }
        // 按顺序回填
        for (int i = 0; i < pdfs.size(); i++) {
            Long uid = pdfs.get(i).getUploadUserId();
            if (uid != null) {
                vos.get(i).setUploadUserName(userNameMap.get(uid));
            }
        }
    }
}
