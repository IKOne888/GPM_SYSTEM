package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.CourseAddDTO;
import com.gmp.dto.CourseUpdateDTO;
import com.gmp.dto.CourseVO;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCourseCategory;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysPosition;
import com.gmp.mapper.TrCourseCategoryMapper;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.mapper.SysPositionMapper;
import com.gmp.service.TrCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程Service实现类
 */
@Service
@RequiredArgsConstructor
public class TrCourseServiceImpl extends ServiceImpl<TrCourseMapper, TrCourse> implements TrCourseService {

    private final TrCourseCategoryMapper categoryMapper;

    /** 部门Mapper：用于回填课程所属部门名称 */
    private final SysDepartmentMapper departmentMapper;

    /** 岗位Mapper：用于回填课程适用岗位名称 */
    private final SysPositionMapper positionMapper;

    @Override
    public Page<CourseVO> pageCourses(Integer pageNum, Integer pageSize, String name, Long categoryId, Integer status) {
        Page<TrCourse> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrCourse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(TrCourse::getName, name);
        }
        if (categoryId != null) {
            wrapper.eq(TrCourse::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(TrCourse::getStatus, status);
        }
        wrapper.orderByDesc(TrCourse::getCreateTime);

        Page<TrCourse> entityPage = baseMapper.selectPage(page, wrapper);

        // 批量加载分类名称，避免 N+1 查询
        Map<Long, String> categoryNameMap = loadCategoryNameMap();
        // 批量加载部门ID->名称映射
        Map<Long, String> departmentNameMap = departmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
        // 批量加载岗位ID->名称映射
        Map<Long, String> positionNameMap = positionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPosition::getId, SysPosition::getName, (a, b) -> a));

        // 转换为 VO 并填充分类/部门/岗位名称
        Page<CourseVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(course -> buildVo(course, categoryNameMap, departmentNameMap, positionNameMap))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<CourseVO> listCourses() {
        // 仅查询启用状态的课程，供下拉选择使用
        LambdaQueryWrapper<TrCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrCourse::getStatus, 1).orderByDesc(TrCourse::getCreateTime);

        // 预算加载名称映射，统一由 buildVo 回填
        Map<Long, String> categoryNameMap = loadCategoryNameMap();
        Map<Long, String> departmentNameMap = departmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
        Map<Long, String> positionNameMap = positionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPosition::getId, SysPosition::getName, (a, b) -> a));

        return baseMapper.selectList(wrapper).stream()
                .map(course -> buildVo(course, categoryNameMap, departmentNameMap, positionNameMap))
                .collect(Collectors.toList());
    }

    /**
     * 将课程实体转换为VO并回填分类/部门/岗位名称
     *
     * @param course           课程实体
     * @param categoryNameMap  分类ID->名称映射
     * @param departmentNameMap 部门ID->名称映射
     * @param positionNameMap  岗位ID->名称映射
     * @return 课程VO
     */
    private CourseVO buildVo(TrCourse course,
                             Map<Long, String> categoryNameMap,
                             Map<Long, String> departmentNameMap,
                             Map<Long, String> positionNameMap) {
        CourseVO vo = CourseVO.from(course);
        vo.setCategoryName(categoryNameMap.getOrDefault(course.getCategoryId(), "未知分类"));
        // 部门/岗位可为空，为空时不回填名称，避免前端显示"未知部门"
        vo.setDepartmentName(course.getDepartmentId() == null ? null
                : departmentNameMap.getOrDefault(course.getDepartmentId(), "未知部门"));
        vo.setPositionName(course.getPositionId() == null ? null
                : positionNameMap.getOrDefault(course.getPositionId(), "未知岗位"));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCourse(CourseAddDTO dto) {
        // 校验课程编码唯一
        checkCodeUnique(dto.getCode(), null);
        // 校验分类存在
        checkCategoryExists(dto.getCategoryId());
        // 校验部门/岗位存在（允许为空）
        checkDepartmentAndPosition(dto.getDepartmentId(), dto.getPositionId());

        TrCourse course = new TrCourse();
        BeanUtils.copyProperties(dto, course);
        baseMapper.insert(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(CourseUpdateDTO dto) {
        // 校验课程存在
        TrCourse exist = baseMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        // 校验课程编码唯一（排除自身）
        checkCodeUnique(dto.getCode(), dto.getId());
        // 校验分类存在
        checkCategoryExists(dto.getCategoryId());
        // 校验部门/岗位存在（允许为空）
        checkDepartmentAndPosition(dto.getDepartmentId(), dto.getPositionId());

        TrCourse course = new TrCourse();
        BeanUtils.copyProperties(dto, course);
        baseMapper.updateById(course);
    }

    /**
     * 校验部门与岗位是否存在（两者均允许为空，非空时才校验）
     *
     * @param departmentId 部门ID
     * @param positionId   岗位ID
     */
    private void checkDepartmentAndPosition(Long departmentId, Long positionId) {
        if (departmentId != null && departmentMapper.selectById(departmentId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属部门不存在");
        }
        if (positionId != null && positionMapper.selectById(positionId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "适用岗位不存在");
        }
    }

    /**
     * 校验课程编码唯一（排除指定ID自身）
     */
    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<TrCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrCourse::getCode, code);
        if (excludeId != null) {
            wrapper.ne(TrCourse::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "课程编码已存在");
        }
    }

    /**
     * 校验课程分类存在且启用
     */
    private void checkCategoryExists(Long categoryId) {
        TrCourseCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程分类不存在");
        }
    }

    /**
     * 加载全部分类ID到名称映射
     */
    private Map<Long, String> loadCategoryNameMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(TrCourseCategory::getId, TrCourseCategory::getName, (a, b) -> a));
    }
}
