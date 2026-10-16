package com.gmp.dto;

import com.gmp.entity.SysUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户信息VO（脱敏，不含密码等敏感字段）
 */
@Data
@ApiModel("用户信息")
public class UserInfoVO {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("部门ID")
    private Long departmentId;

    @ApiModelProperty("岗位ID")
    private Long positionId;

    @ApiModelProperty("是否部门负责人：0-否 1-是")
    private Integer isDepartmentLeader;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("角色编码列表（如 SUPER_ADMIN），用于前端权限控制")
    private List<String> roles;

    /**
     * 从实体构建（不含密码）
     *
     * @param user 用户实体
     */
    public static UserInfoVO from(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setPositionId(user.getPositionId());
        vo.setIsDepartmentLeader(user.getIsDepartmentLeader());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}
