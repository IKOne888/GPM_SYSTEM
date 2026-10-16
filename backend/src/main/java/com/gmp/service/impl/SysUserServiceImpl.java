package com.gmp.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.RoleLevel;
import com.gmp.dto.LoginVO;
import com.gmp.dto.UserAddDTO;
import com.gmp.dto.UserInfoVO;
import com.gmp.dto.UserListVO;
import com.gmp.dto.UserUpdateDTO;
import com.gmp.dto.auth.AdminPasswordUpdateDTO;
import com.gmp.dto.auth.AdminProfileUpdateDTO;
import com.gmp.dto.auth.AdminProfileVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysPosition;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysUser;
import com.gmp.entity.SysUserRole;
import com.gmp.mapper.SysRoleMapper;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.SysUserRoleMapper;
import com.gmp.service.SysDepartmentService;
import com.gmp.service.SysPositionService;
import com.gmp.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /** 密码强度正则：至少 6 位，且同时包含字母和数字 */
    private static final java.util.regex.Pattern PASSWORD_PATTERN = java.util.regex.Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,20}$");

    /** 默认重置密码（满足密码强度要求：6-20位含字母和数字） */
    private static final String DEFAULT_PASSWORD = "Gmp@123456";

    /** 用户角色关联Mapper */
    private final SysUserRoleMapper userRoleMapper;

    /** 角色Mapper */
    private final SysRoleMapper roleMapper;

    /** 部门Service（用于列表VO回填部门名称） */
    private final SysDepartmentService departmentService;

    /** 岗位Service（用于列表VO回填岗位名称） */
    private final SysPositionService positionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(String username, String password) {
        SysUser user = baseMapper.selectByUsername(username);
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            // 用户不存在与密码错误统一提示，避免枚举出合法用户名
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        // 先校验禁用状态，再登录，避免禁用账号产生会话
        StpUtil.checkDisable(user.getId());
        StpUtil.login(user.getId());

        // 记录最后登录时间与IP
        updateLastLoginInfo(user);

        // 构建登录返回信息（含角色编码列表，供前端做菜单/按钮权限控制）
        UserInfoVO userInfo = UserInfoVO.from(user);
        userInfo.setRoles(getRoleCodes(user.getId()));
        return new LoginVO(StpUtil.getTokenValue(), userInfo);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 更新最后登录时间与IP
     */
    private void updateLastLoginInfo(SysUser user) {
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(getClientIp());
        baseMapper.updateById(update);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(ip)) {
                    // X-Forwarded-For 可能包含多个IP，取第一个（客户端原始IP）
                    String firstIp = ip.split(",")[0].trim();
                    if (!"unknown".equalsIgnoreCase(firstIp)) {
                        return firstIp;
                    }
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("获取客户端IP失败：{}", e.getMessage());
        }
        return null;
    }

    @Override
    public Page<UserListVO> pageUsers(Integer pageNum, Integer pageSize, String username, Long departmentId, Integer status) {
        // 1. 分页查询用户实体
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (departmentId != null) {
            wrapper.eq(SysUser::getDepartmentId, departmentId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        baseMapper.selectPage(page, wrapper);

        // 2. 组装VO（部门名/岗位名/角色信息）
        Page<UserListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserListVO> voList = page.getRecords().stream()
                .map(this::buildUserListVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 构建用户列表VO（回填部门名、岗位名、角色信息）
     */
    private UserListVO buildUserListVO(SysUser user) {
        UserListVO vo = new UserListVO();
        BeanUtils.copyProperties(user, vo);
        // 回填部门名称
        if (user.getDepartmentId() != null) {
            SysDepartment department = departmentService.getById(user.getDepartmentId());
            if (department != null) {
                vo.setDepartmentName(department.getName());
            }
        }
        // 回填岗位名称
        if (user.getPositionId() != null) {
            SysPosition position = positionService.getById(user.getPositionId());
            if (position != null) {
                vo.setPositionName(position.getName());
            }
        }
        // 回填角色信息
        vo.setRoleIds(getRoleIds(user.getId()));
        vo.setRoleCodes(getRoleCodes(user.getId()));
        vo.setRoleNames(getRoleNames(user.getId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserAddDTO dto) {
        // 1. 确定待分配角色：前端未指定角色时，默认赋予"普通员工"角色
        List<Long> roleIds = dto.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = Collections.singletonList(getRoleIdByCode(RoleLevel.EMPLOYEE));
        }
        // 校验当前登录人角色等级是否足以分配目标角色
        validateAssignRoles(roleIds);

        // 2. 校验密码强度
        validatePassword(dto.getPassword());

        // 3. 用户名唯一性校验
        SysUser existUser = baseMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        // 4. 插入用户（密码加密存储）
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        baseMapper.insert(user);

        // 5. 分配角色
        assignRoles(user.getId(), roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        // 1. 校验目标用户存在
        SysUser existUser = baseMapper.selectById(dto.getId());
        if (existUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 2. 权限校验：操作者等级必须高于目标用户等级，且不能操作自己
        checkManagePermission(dto.getId());

        // 3. 仅在显式传入角色时才校验与重分配，未传则保留用户原有角色
        List<Long> roleIds = dto.getRoleIds();
        boolean updateRoles = roleIds != null && !roleIds.isEmpty();
        if (updateRoles) {
            validateAssignRoles(roleIds);
        }

        // 4. 用户名唯一性校验（排除自身）
        SysUser sameNameUser = baseMapper.selectByUsername(dto.getUsername());
        if (sameNameUser != null && !sameNameUser.getId().equals(dto.getId())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        // 5. 更新基本信息（密码不可通过此接口修改）
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(null);
        baseMapper.updateById(user);

        // 6. 重新分配角色（先删后插）
        if (updateRoles) {
            assignRoles(dto.getId(), roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 权限校验（含"不能操作自己"与等级校验）
        checkManagePermission(id);
        // 删除用户（逻辑删除）并清理角色关联
        baseMapper.deleteById(id);
        deleteUserRoles(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要删除的用户");
        }
        // 逐条权限校验，防止越权删除
        for (Long id : ids) {
            checkManagePermission(id);
        }
        for (Long id : ids) {
            baseMapper.deleteById(id);
            deleteUserRoles(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态参数不合法");
        }
        // 权限校验（含"不能操作自己"与等级校验）
        checkManagePermission(id);
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id , String newPassword) {
        // 权限校验（含"不能操作自己"与等级校验）
        checkManagePermission(id);
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        baseMapper.updateById(user);
    }

    @Override
    public List<SysRole> listRoles() {
        // 仅返回启用状态的角色，供前端角色下拉选择
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
    }

    @Override
    public List<com.gmp.dto.UserSimpleVO> listUsers() {
        // 仅返回启用状态的用户，脱敏为 id/realName/username，供执行人下拉选择
        List<SysUser> users = baseMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        return users.stream().map(u -> {
            com.gmp.dto.UserSimpleVO vo = new com.gmp.dto.UserSimpleVO();
            vo.setId(u.getId());
            vo.setRealName(u.getRealName());
            vo.setUsername(u.getUsername());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 权限分层校验：当前登录人是否有权管理目标用户
     * <p>
     * 规则：不能操作自己的账号；操作者等级必须严格高于目标用户等级。
     * 系统管理员(100)可管理所有用户（含管理员）；管理员(90)只能管理普通用户(10)。
     *
     * @param targetUserId 目标用户ID
     */
    private void checkManagePermission(Long targetUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        // 不能操作自己的账号
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_OPERATE_SELF, "不能操作自己的账号");
        }
        // 目标用户必须存在
        SysUser targetUser = baseMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 等级校验：操作者等级必须高于目标用户等级
        int currentLevel = getMaxLevelOfUser(currentUserId);
        int targetLevel = getMaxLevelOfUser(targetUserId);
        if (currentLevel <= targetLevel) {
            throw new BusinessException(ErrorCode.ROLE_LEVEL_DENIED, "无权管理等级不低于自己的用户");
        }
    }

    /**
     * 校验当前登录人是否有权分配指定角色
     * <p>
     * 规则：仅系统管理员(100)可分配包括 SUPER_ADMIN 在内的任意角色；
     * 管理员(90)只能分配等级低于自己的角色（即普通用户角色）。
     *
     * @param roleIds 待分配的角色ID列表
     */
    private void validateAssignRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少分配一个角色");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        int currentLevel = getMaxLevelOfUser(currentUserId);
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role == null || role.getStatus() == null || role.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不存在或已停用：" + roleId);
            }
            int roleLevel = RoleLevel.getLevel(role.getCode());
            // 非系统管理员不能分配等级不低于自己的角色
            if (currentLevel < RoleLevel.LEVEL_SUPER_ADMIN && roleLevel >= currentLevel) {
                throw new BusinessException(ErrorCode.ROLE_LEVEL_DENIED,
                        "无权分配等级不低于自己的角色：" + role.getName());
            }
        }
    }

    /**
     * 为用户分配角色（先删后插，保证全量覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 先删除原有关联
        deleteUserRoles(userId);
        // 再批量插入新关联
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    /**
     * 删除用户的全部角色关联
     */
    private void deleteUserRoles(Long userId) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    /**
     * 根据角色编码查询角色ID（用于分配默认角色）
     */
    private Long getRoleIdByCode(String code) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code)
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "默认角色不存在：" + code);
        }
        return role.getId();
    }

    /**
     * 获取用户的角色ID列表
     */
    private List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)).stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的角色编码列表（按角色ID升序）
     */
    private List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getCode)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的角色名称列表
     */
    private List<String> getRoleNames(Long userId) {
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户所有角色中的最高等级（无角色返回0）
     */
    private int getMaxLevelOfUser(Long userId) {
        List<String> roleCodes = getRoleCodes(userId);
        int maxLevel = 0;
        for (String roleCode : roleCodes) {
            int level = RoleLevel.getLevel(roleCode);
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }

    /**
     * 校验密码强度
     *
     * @param password 明文密码
     */
    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码需为6-20位，且同时包含字母和数字");
        }
    }

    // ==================== 个人中心（登录用户自助维护） ====================

    @Override
    public AdminProfileVO currentProfile() {
        // 取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 组装个人资料（不含密码等敏感字段）
        AdminProfileVO vo = new AdminProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setPositionId(user.getPositionId());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        // 角色编码列表（用于前端权限控制与标签展示）
        vo.setRoles(getRoleCodes(userId));
        // 回填部门名称
        if (user.getDepartmentId() != null) {
            SysDepartment dept = departmentService.getById(user.getDepartmentId());
            vo.setDepartmentName(dept == null ? null : dept.getName());
        }
        // 回填岗位名称
        if (user.getPositionId() != null) {
            SysPosition position = positionService.getById(user.getPositionId());
            vo.setPositionName(position == null ? null : position.getName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentProfile(AdminProfileUpdateDTO dto) {
        // 取当前登录用户，仅更新本人非敏感资料
        Long userId = StpUtil.getLoginIdAsLong();
        if (baseMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(dto.getRealName());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentPassword(AdminPasswordUpdateDTO dto) {
        // 取当前登录用户，仅允许修改本人密码
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 两次输入的新密码必须一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        // 原密码必须匹配
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原密码错误");
        }
        // 新密码不得与原密码相同
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "新密码不能与原密码相同");
        }
        // 兜底校验密码强度（DTO 已校验，此处防止绕过）
        validatePassword(dto.getNewPassword());

        SysUser update = new SysUser();
        update.setId(userId);
        // 以 BCrypt 密文存储
        update.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        baseMapper.updateById(update);
    }
}
