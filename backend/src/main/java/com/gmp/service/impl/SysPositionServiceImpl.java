package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.PositionMatrixVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysPosition;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCourseCategory;
import com.gmp.entity.TrPositionCourseMatrix;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysPositionMapper;
import com.gmp.mapper.TrCourseCategoryMapper;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.TrPositionCourseMatrixMapper;
import com.gmp.service.SysPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysPositionServiceImpl extends ServiceImpl<SysPositionMapper, SysPosition> implements SysPositionService {

    /** 部门Mapper（回填部门名称） */
    private final SysDepartmentMapper departmentMapper;

    /** 课程Mapper（构建培训矩阵） */
    private final TrCourseMapper courseMapper;

    /** 课程分类Mapper（回填分类名称） */
    private final TrCourseCategoryMapper categoryMapper;

    /** 岗位培训矩阵Mapper */
    private final TrPositionCourseMatrixMapper matrixMapper;

    @Override
    public List<SysPosition> listPositions(String keyword) {
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysPosition::getName, keyword);
        }
        wrapper.orderByAsc(SysPosition::getId);
        List<SysPosition> list = baseMapper.selectList(wrapper);
        fillDepartmentName(list);
        return list;
    }

    @Override
    public Page<SysPosition> pagePositions(Integer pageNum, Integer pageSize, String name, Integer status) {
        Page<SysPosition> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(SysPosition::getName, name);
        }
        if (status != null) {
            wrapper.eq(SysPosition::getStatus, status);
        }
        wrapper.orderByAsc(SysPosition::getId);
        baseMapper.selectPage(page, wrapper);
        // 回填部门名称，供列表展示
        fillDepartmentName(page.getRecords());
        return page;
    }

    /**
     * 批量回填岗位的所属部门名称
     */
    private void fillDepartmentName(List<SysPosition> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> deptIds = list.stream()
                .map(SysPosition::getDepartmentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> deptNameMap = departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName));
        for (SysPosition position : list) {
            if (position.getDepartmentId() != null) {
                position.setDepartmentName(deptNameMap.get(position.getDepartmentId()));
            }
        }
    }

    @Override
    public List<PositionMatrixVO> getTrainingMatrix(Long positionId) {
        // 1. 校验岗位存在
        if (baseMapper.selectById(positionId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        // 2. 查询全部启用课程
        List<TrCourse> courses = courseMapper.selectList(new LambdaQueryWrapper<TrCourse>()
                .eq(TrCourse::getStatus, 1)
                .orderByAsc(TrCourse::getId));
        // 3. 分类ID -> 名称
        Map<Long, String> categoryNameMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TrCourseCategory::getId, TrCourseCategory::getName));
        // 4. 当前岗位已配置的矩阵（courseId -> 记录）
        Map<Long, TrPositionCourseMatrix> matrixMap = new HashMap<>();
        for (TrPositionCourseMatrix matrix : matrixMapper.selectList(new LambdaQueryWrapper<TrPositionCourseMatrix>()
                .eq(TrPositionCourseMatrix::getPositionId, positionId))) {
            matrixMap.put(matrix.getCourseId(), matrix);
        }
        // 5. 组装行
        List<PositionMatrixVO> rows = new ArrayList<>();
        for (TrCourse course : courses) {
            PositionMatrixVO vo = new PositionMatrixVO();
            vo.setCourseId(course.getId());
            vo.setCourseName(course.getName());
            vo.setCategory(course.getCategoryId() == null ? null : categoryNameMap.get(course.getCategoryId()));
            TrPositionCourseMatrix matrix = matrixMap.get(course.getId());
            if (matrix != null) {
                // is_required=1 视为必修，=0 视为选修
                boolean required = matrix.getIsRequired() != null && matrix.getIsRequired() == 1;
                vo.setRequired(required);
                vo.setElective(!required);
                vo.setFrequency(matrix.getFrequency());
            } else {
                vo.setRequired(false);
                vo.setElective(false);
                vo.setFrequency(null);
            }
            rows.add(vo);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrainingMatrix(Long positionId, List<PositionMatrixVO> rows) {
        // 1. 校验岗位存在
        if (baseMapper.selectById(positionId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        // 2. 先清空该岗位原有矩阵配置（全量覆盖）
        matrixMapper.delete(new LambdaQueryWrapper<TrPositionCourseMatrix>()
                .eq(TrPositionCourseMatrix::getPositionId, positionId));
        // 3. 逐行保存有配置的课程
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (PositionMatrixVO row : rows) {
            if (row.getCourseId() == null) {
                continue;
            }
            boolean hasConfig = Boolean.TRUE.equals(row.getRequired())
                    || Boolean.TRUE.equals(row.getElective())
                    || StringUtils.hasText(row.getFrequency());
            if (!hasConfig) {
                continue;
            }
            TrPositionCourseMatrix matrix = new TrPositionCourseMatrix();
            matrix.setPositionId(positionId);
            matrix.setCourseId(row.getCourseId());
            matrix.setIsRequired(Boolean.TRUE.equals(row.getRequired()) ? 1 : 0);
            matrix.setFrequency(row.getFrequency());
            matrixMapper.insert(matrix);
        }
    }
}
