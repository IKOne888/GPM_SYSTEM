package com.gmp.common;

/**
 * 角色等级常量类
 * <p>
 * 在现有业务角色基础上定义三级权限等级，等级数字越大权限越高：
 * - 系统管理员(SUPER_ADMIN，100)：最高权限，可管理所有用户和管理员
 * - 管理员(QA_MANAGER/TRAINING_ADMIN，90)：可管理普通用户，不可管理其他管理员
 * - 普通用户(DEPT_LEADER/EMPLOYEE，10)：无用户管理权限
 * <p>
 * 管理规则：操作者只能管理"等级严格低于自己"的用户；
 * 系统管理员可管理所有用户（含管理员），但不可管理自身。
 */
public class RoleLevel {

    /** 系统管理员编码（最高权限） */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** QA负责人编码（管理员级别） */
    public static final String QA_MANAGER = "QA_MANAGER";

    /** 培训管理员编码（管理员级别） */
    public static final String TRAINING_ADMIN = "TRAINING_ADMIN";

    /** 部门负责人编码（普通用户级别） */
    public static final String DEPT_LEADER = "DEPT_LEADER";

    /** 普通员工编码（普通用户级别） */
    public static final String EMPLOYEE = "EMPLOYEE";

    /** 系统管理员等级 */
    public static final int LEVEL_SUPER_ADMIN = 100;

    /** 管理员等级 */
    public static final int LEVEL_ADMIN = 90;

    /** 普通用户等级 */
    public static final int LEVEL_USER = 10;

    private RoleLevel() {
    }

    /**
     * 根据角色编码获取等级
     *
     * @param roleCode 角色编码
     * @return 等级，未知角色返回 0
     */
    public static int getLevel(String roleCode) {
        if (roleCode == null) {
            return 0;
        }
        switch (roleCode) {
            case SUPER_ADMIN:
                return LEVEL_SUPER_ADMIN;
            case QA_MANAGER:
            case TRAINING_ADMIN:
                return LEVEL_ADMIN;
            case DEPT_LEADER:
            case EMPLOYEE:
                return LEVEL_USER;
            default:
                return 0;
        }
    }
}
