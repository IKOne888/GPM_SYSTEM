package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通知 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，提供消息列表、未读数与已读标记能力。
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
