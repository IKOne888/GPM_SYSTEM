package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.dto.role.*;
import com.gmp.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色Service接口
 */
public interface RoleService extends IService<SysRole> {

    /**
     * 分页查询角色列表（支持按名称/编码/状态模糊过滤，回填 userCount）
     *
     * @param name     角色名称（可选，模糊查询）
     * @param code     角色编码（可选，模糊查询）
     * @param status   状态（可选，精确查询）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Page<RoleVO> pageRoles(String name, String code, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询全部启用角色（供下拉选择使用）
     *
     * @return 角色列表
     */
    List<RoleVO> listAll();

    /**
     * 新增角色（校验 code 唯一）
     *
     * @param dto 新增请求
     * @return 新角色ID
     */
    Long addRole(RoleSaveDTO dto);

    /**
     * 修改角色（code 不允许修改）
     *
     * @param dto 修改请求
     */
    void updateRole(RoleUpdateDTO dto);

    /**
     * 逻辑删除角色（先检查是否有用户关联，有则拒绝删除）
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);

    /**
     * 切换角色启用/禁用状态
     *
     * @param id     角色ID
     * @param status 目标状态：0-禁用, 1-启用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 构建菜单树（从 sys_menu 表递归组装，仅返回启用的菜单）
     *
     * @return 菜单树根节点列表
     */
    List<MenuTreeVO> getMenuTree();

    /**
     * 查询指定角色当前已分配的菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> listRoleMenuIds(Long roleId);

    /**
     * 全量替换式绑定菜单（先删后插，事务保证）
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表（空列表表示清除所有绑定）
     */
    void bindMenus(Long roleId, List<Long> menuIds);
}
