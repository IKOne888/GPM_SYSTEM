package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体类
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级菜单ID（0为顶级） */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 菜单图标 */
    private String icon;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 菜单类型：1-目录, 2-菜单, 3-按钮 */
    private Integer type;

    /** 权限标识（如 system:user:add） */
    private String permission;

    /** 是否可见：0-隐藏, 1-显示 */
    private Integer visible;

    /** 状态：0-禁用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
