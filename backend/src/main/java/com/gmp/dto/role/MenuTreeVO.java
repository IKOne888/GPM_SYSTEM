package com.gmp.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 菜单树VO（供前端权限分配弹窗使用）
 */
@Data
@ApiModel("菜单树节点")
public class MenuTreeVO {

    /** 菜单ID */
    @ApiModelProperty("菜单ID")
    private Long id;

    /** 父级菜单ID（0为顶级） */
    @ApiModelProperty("父级菜单ID")
    private Long parentId;

    /** 菜单名称 */
    @ApiModelProperty("菜单名称")
    private String name;

    /** 路由路径 */
    @ApiModelProperty("路由路径")
    private String path;

    /** 组件路径 */
    @ApiModelProperty("组件路径")
    private String component;

    /** 菜单图标 */
    @ApiModelProperty("菜单图标")
    private String icon;

    /** 排序号 */
    @ApiModelProperty("排序号")
    private Integer sort;

    /** 菜单类型：1-目录, 2-菜单, 3-按钮 */
    @ApiModelProperty("菜单类型：1-目录, 2-菜单, 3-按钮")
    private Integer type;

    /** 权限标识 */
    @ApiModelProperty("权限标识")
    private String permission;

    /** 是否可见：0-隐藏, 1-显示 */
    @ApiModelProperty("是否可见")
    private Integer visible;

    /** 子菜单列表 */
    @ApiModelProperty("子菜单")
    private List<MenuTreeVO> children;

    /** 是否有子节点（用于前端展开状态） */
    @ApiModelProperty("是否有子节点")
    private Boolean hasChildren;
}
