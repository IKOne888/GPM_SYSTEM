package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.audit.AuditLogExcelVO;
import com.gmp.dto.audit.AuditLogVO;
import com.gmp.entity.SysAuditLog;

import java.util.List;

/**
 * 审计日志 Service 接口
 */
public interface SysAuditLogService extends IService<SysAuditLog> {

    /**
     * 分页查询审计日志
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param module    操作模块（前端枚举，空表示不过滤）
     * @param action    操作类型（前端枚举，空表示不过滤）
     * @param operator  操作人（模糊匹配姓名或账号）
     * @param startDate 开始日期（yyyy-MM-dd，可空）
     * @param endDate   结束日期（yyyy-MM-dd，可空）
     * @return 分页结果
     */
    Page<AuditLogVO> pageLogs(Integer pageNum, Integer pageSize, String module, String action,
                              String operator, String startDate, String endDate);

    /**
     * 查询审计日志导出数据
     *
     * @param module   操作模块
     * @param action   操作类型
     * @param operator 操作人
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 导出模型列表
     */
    List<AuditLogExcelVO> listForExport(String module, String action, String operator,
                                        String startDate, String endDate);
}
