package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.entity.SysDepartment;
import com.gmp.mapper.SysDepartmentMapper;
import com.gmp.service.SysDepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门Service实现类
 */
@Service
public class SysDepartmentServiceImpl extends ServiceImpl<SysDepartmentMapper, SysDepartment> implements SysDepartmentService {

    @Override
    public List<SysDepartment> listDepartments(String keyword) {
        LambdaQueryWrapper<SysDepartment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysDepartment::getName, keyword);
        }
        wrapper.orderByAsc(SysDepartment::getSort);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<SysDepartment> listTree() {
        // 查询全部部门（按排序升序），组装为树形结构
        List<SysDepartment> all = baseMapper.selectList(new LambdaQueryWrapper<SysDepartment>()
                .orderByAsc(SysDepartment::getSort));
        // 使用 LinkedHashMap 保持查询顺序
        Map<Long, SysDepartment> map = new LinkedHashMap<>();
        for (SysDepartment dept : all) {
            dept.setChildren(new ArrayList<>());
            map.put(dept.getId(), dept);
        }
        List<SysDepartment> roots = new ArrayList<>();
        for (SysDepartment dept : all) {
            Long parentId = dept.getParentId();
            // 顶级部门：parentId 为空/0，或父部门不存在
            if (parentId == null || parentId == 0L || !map.containsKey(parentId)) {
                roots.add(dept);
            } else {
                map.get(parentId).getChildren().add(dept);
            }
        }
        return roots;
    }

    @Override
    public Page<SysDepartment> pageDepartments(Integer pageNum, Integer pageSize, String keyword) {
        Page<SysDepartment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDepartment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysDepartment::getName, keyword);
        }
        wrapper.orderByAsc(SysDepartment::getSort);
        return baseMapper.selectPage(page, wrapper);
    }
}
