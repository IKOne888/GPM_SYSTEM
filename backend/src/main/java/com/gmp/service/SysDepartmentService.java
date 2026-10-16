package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.entity.SysDepartment;

import java.util.List;

/**
 * 部门Service接口
 */
public interface SysDepartmentService extends IService<SysDepartment> {

    /**
     * 查询部门列表
     */
    List<SysDepartment> listDepartments(String keyword);

    /**
     * 查询部门树（按父子关系组装 children）
     */
    List<SysDepartment> listTree();

    /**
     * 分页查询部门
     */
    Page<SysDepartment> pageDepartments(Integer pageNum, Integer pageSize, String keyword);
}
