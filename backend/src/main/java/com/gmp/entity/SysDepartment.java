package com.gmp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门实体类
 */
@Data
@TableName("sys_department")
public class SysDepartment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    private Long parentId;

    private Integer sort;

    private Integer status;

    /** 子部门（非数据库字段，部门树回填用） */
    @TableField(exist = false)
    private List<SysDepartment> children;

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
