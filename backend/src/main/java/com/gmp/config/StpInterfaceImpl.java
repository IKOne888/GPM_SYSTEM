package com.gmp.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysUserRole;
import com.gmp.mapper.SysRoleMapper;
import com.gmp.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限数据源实现类
 * <p>
 * 为 Sa-Token 的 checkRole / checkPermission 提供当前登录用户的角色编码与权限码列表，
 * 数据来源为 sys_user_role（用户-角色关联表）与 sys_role（角色表）。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    /** 用户角色关联Mapper */
    private final SysUserRoleMapper userRoleMapper;

    /** 角色Mapper */
    private final SysRoleMapper roleMapper;

    /**
     * 获取当前登录用户的权限码列表
     * <p>
     * 本项目采用"角色 + 接口路由校验"的权限模型，暂不按权限码细粒度控制，
     * 故返回空列表；如需菜单按钮级权限码可在此扩展。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 获取当前登录用户的角色编码列表（如 SUPER_ADMIN、QA_MANAGER）
     *
     * @param loginId   登录会话ID（即用户ID）
     * @param loginType 登录类型
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(String.valueOf(loginId));
        // 1. 查询该用户关联的全部角色ID
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        // 2. 批量查询角色实体并返回启用状态的角色编码
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getCode)
                .collect(Collectors.toList());
    }
}
