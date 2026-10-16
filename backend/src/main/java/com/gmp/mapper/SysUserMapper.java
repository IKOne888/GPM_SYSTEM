package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据部门ID查询用户列表
     */
    List<SysUser> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 根据岗位ID查询用户列表
     */
    List<SysUser> selectByPositionId(@Param("positionId") Long positionId);
}
