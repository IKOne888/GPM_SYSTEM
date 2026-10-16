package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计追踪Mapper接口
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
