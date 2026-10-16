package com.gmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.role.*;
import com.gmp.entity.SysMenu;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysRoleMenu;
import com.gmp.entity.SysUserRole;
import com.gmp.mapper.SysMenuMapper;
import com.gmp.mapper.SysRoleMapper;
import com.gmp.mapper.SysRoleMenuMapper;
import com.gmp.mapper.SysUserRoleMapper;
import com.gmp.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色Service实现类
 * <p>
 * 负责角色的增删改查、菜单权限绑定、用户关联检查等核心业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    /** 菜单Mapper */
    private final SysMenuMapper menuMapper;

    /** 角色菜单关联Mapper */
    private final SysRoleMenuMapper roleMenuMapper;

    /** 用户角色关联Mapper（用于检查角色是否被用户使用） */
    private final SysUserRoleMapper userRoleMapper;

    // ============== 角色 CRUD ==============

    @Override
    public Page<RoleVO> pageRoles(String name, String code, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 构建分页查询条件
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        // 角色名称模糊匹配
        if (StringUtils.hasText(name)) {
            wrapper.like(SysRole::getName, name);
        }
        // 角色编码模糊匹配
        if (StringUtils.hasText(code)) {
            wrapper.like(SysRole::getCode, code);
        }
        // 状态精确匹配
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        // 按ID升序排列
        wrapper.orderByAsc(SysRole::getId);

        // 2. 执行分页查询
        baseMapper.selectPage(page, wrapper);

        // 3. 批量回填 userCount（避免 N+1 问题），获取角色ID->用户数映射
        List<SysRole> records = page.getRecords();
        Map<Long, Long> countMap = records.isEmpty() ? Collections.emptyMap() : getUserCountMap(records);

        // 4. 转换为 VO 并返回分页结果
        Page<RoleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records.stream().map(r -> convertToVO(r, countMap)).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<RoleVO> listAll() {
        // 仅查询启用状态的角色，供下拉选择使用
        List<SysRole> roles = baseMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
        // 批量查询用户数量（避免 N+1）
        Map<Long, Long> countMap = roles.isEmpty() ? Collections.emptyMap() : getUserCountMap(roles);
        return roles.stream().map(r -> convertToVO(r, countMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRole(RoleSaveDTO dto) {
        // 1. 校验角色编码唯一性
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, dto.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码已存在");
        }

        // 2. 构建实体并保存
        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        baseMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateDTO dto) {
        // 1. 校验角色是否存在
        SysRole existing = baseMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 2. 构建更新对象（code 不允许修改，由 BeanUtils.copyProperties 自动跳过 null 字段）
        SysRole role = new SysRole();
        role.setId(dto.getId());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        baseMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 1. 校验角色是否存在
        SysRole existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 2. 检查是否有用户正在使用该角色
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "该角色已被 " + userCount + " 个用户使用，无法删除");
        }

        // 3. 检查是否有菜单权限绑定（清理孤立数据）
        long menuCount = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        if (menuCount > 0) {
            // 先删除角色菜单关联
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, id));
        }

        // 4. 执行逻辑删除
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        // 1. 校验角色是否存在
        SysRole existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 2. 更新状态
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        baseMapper.updateById(role);
    }

    // ============== 菜单权限相关 ==============

    @Override
    public List<MenuTreeVO> getMenuTree() {
        // 1. 查询所有启用状态的菜单（按父ID和排序号升序）
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort));

        // 2. 将菜单转换为 VO 并构建树形结构
        if (CollectionUtils.isEmpty(allMenus)) {
            return Collections.emptyList();
        }

        // 按 parentId 分组
        Map<Long, List<SysMenu>> parentMap = allMenus.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getParentId() == null ? 0L : m.getParentId()
                ));

        // 转换为 VO
        Map<Long, MenuTreeVO> voMap = new HashMap<>();
        for (SysMenu menu : allMenus) {
            voMap.put(menu.getId(), convertToMenuTreeVO(menu));
        }

        // 组装树：为每个节点设置 children
        for (SysMenu menu : allMenus) {
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            List<SysMenu> children = parentMap.getOrDefault(menu.getId(), Collections.emptyList());
            MenuTreeVO vo = voMap.get(menu.getId());
            if (!children.isEmpty()) {
                List<MenuTreeVO> childVOs = children.stream()
                        .map(c -> voMap.get(c.getId()))
                        .collect(Collectors.toList());
                vo.setChildren(childVOs);
                vo.setHasChildren(true);
            } else {
                vo.setChildren(Collections.emptyList());
                vo.setHasChildren(false);
            }
        }

        // 3. 返回顶级菜单（parentId 为 0 或 null）
        List<MenuTreeVO> rootMenus = parentMap.getOrDefault(0L, Collections.emptyList())
                .stream()
                .map(m -> voMap.get(m.getId()))
                .collect(Collectors.toList());

        return rootMenus;
    }

    @Override
    public List<Long> listRoleMenuIds(Long roleId) {
        // 校验角色是否存在
        SysRole existing = baseMapper.selectById(roleId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 查询角色菜单关联
        List<SysRoleMenu> bindings = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));

        // 返回菜单ID列表
        return bindings.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindMenus(Long roleId, List<Long> menuIds) {
        // 1. 校验角色是否存在
        SysRole existing = baseMapper.selectById(roleId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        // 2. 校验传入的菜单ID是否都存在
        if (!CollectionUtils.isEmpty(menuIds)) {
            List<SysMenu> validMenus = menuMapper.selectBatchIds(menuIds);
            Set<Long> validIds = validMenus.stream().map(SysMenu::getId).collect(Collectors.toSet());
            // 找出无效的菜单ID
            List<Long> invalidIds = menuIds.stream()
                    .filter(id -> !validIds.contains(id))
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "以下菜单ID不存在: " + invalidIds);
            }
        }

        // 3. 先删除原有的角色菜单关联（全量替换策略）
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));

        // 4. 批量插入新的关联记录
        if (!CollectionUtils.isEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                SysRoleMenu binding = new SysRoleMenu();
                binding.setRoleId(roleId);
                binding.setMenuId(menuId);
                roleMenuMapper.insert(binding);
            }
        }
    }

    // ============== 私有辅助方法 ==============

    /**
     * 批量查询指定角色的关联用户数量（避免 N+1 查询）
     *
     * @param roles 角色列表
     * @return 角色ID -> 用户数量 的映射
     */
    private Map<Long, Long> getUserCountMap(List<SysRole> roles) {
        // 收集所有角色ID
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        // 一次性查询所有与这些角色相关的用户角色关联记录
        List<SysUserRole> allBindings = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, roleIds));

        // 按 roleId 分组计数
        return allBindings.stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getRoleId,
                        Collectors.counting()
                ));
    }

    /**
     * 将 SysRole 转换为 RoleVO，同时回填关联用户数
     *
     * @param role     角色实体
     * @param countMap 角色ID -> 用户数量 的映射
     * @return 角色 VO
     */
    private RoleVO convertToVO(SysRole role, Map<Long, Long> countMap) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        // 从 countMap 中获取 userCount，不存在则为 0
        vo.setUserCount(countMap.getOrDefault(role.getId(), 0L).intValue());
        return vo;
    }

    /**
     * 将 SysMenu 转换为 MenuTreeVO
     */
    private MenuTreeVO convertToMenuTreeVO(SysMenu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }
}
