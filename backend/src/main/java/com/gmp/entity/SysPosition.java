package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 岗位实体类
 */
@Data
@TableName("sys_position")
public class SysPosition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /** 所属部门ID（岗位归属部门，实现"部门-岗位"两级分类） */
    private Long departmentId;

    /** 岗位级别：初级/中级/高级/专家 */
    private String level;

    private String description;

    private Integer status;

    /** 所属部门名称（非数据库字段，列表回填用） */
    @TableField(exist = false)
    private String departmentName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    private Integer deleted;
}
