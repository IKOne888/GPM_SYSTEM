package com.gmp.service;

import com.gmp.dto.LoginVO;
import com.gmp.dto.UserAddDTO;
import com.gmp.dto.UserListVO;
import com.gmp.dto.UserSimpleVO;
import com.gmp.dto.UserUpdateDTO;
import com.gmp.dto.auth.AdminPasswordUpdateDTO;
import com.gmp.dto.auth.AdminProfileUpdateDTO;
import com.gmp.dto.auth.AdminProfileVO;
import com.gmp.entity.SysRole;
import com.gmp.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户Service接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（令牌 + 用户信息，含角色编码）
     */
    LoginVO login(String username, String password);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 分页查询用户（返回含部门名/岗位名/角色信息的VO）
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param username     用户名模糊查询（可选）
     * @param departmentId 部门ID精确查询（可选）
     * @param status       状态查询（可选）
     */
    Page<UserListVO> pageUsers(Integer pageNum, Integer pageSize, String username, Long departmentId, Integer status);

    /**
     * 新增用户（含角色分配）
     */
    void addUser(UserAddDTO dto);

    /**
     * 修改用户（含角色重新分配）
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 删除用户（含角色关联清理，需权限校验）
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户（逐条权限校验）
     */
    void batchDelete(List<Long> ids);

    /**
     * 修改用户状态（启用/禁用，需权限校验）
     */
    void changeStatus(Long id, Integer status);

    /**
     * 重置用户密码为默认密码（需权限校验）
     */
    void resetPassword(Long id , String  newPassword);

    /**
     * 查询启用状态的角色列表（用于前端角色下拉）
     */
    List<SysRole> listRoles();

    /**
     * 查询启用状态的用户下拉列表（用于任务分配、执行人选择，脱敏返回）
     *
     * @return 用户下拉选项列表
     */
    List<UserSimpleVO> listUsers();

    // ==================== 个人中心（登录用户自助维护） ====================

    /**
     * 查询当前登录用户的个人资料
     * <p>
     * 返回部门名称、岗位名称与角色编码列表，供管理端「个人中心」直接展示。
     *
     * @return 个人资料
     */
    AdminProfileVO currentProfile();

    /**
     * 修改当前登录用户的个人资料（姓名 / 手机号 / 邮箱）
     *
     * @param dto 修改参数
     */
    void updateCurrentProfile(AdminProfileUpdateDTO dto);

    /**
     * 修改当前登录用户的登录密码（需校验原密码与两次输入一致性）
     *
     * @param dto 修改密码参数
     */
    void updateCurrentPassword(AdminPasswordUpdateDTO dto);
}
