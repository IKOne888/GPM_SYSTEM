package com.gmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmp.entity.TrCoursePdf;
import org.apache.ibatis.annotations.Mapper;

/**
 * PDF 课件 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，获得单表 CRUD 与分页能力，无需编写 XML。
 */
@Mapper
public interface TrCoursePdfMapper extends BaseMapper<TrCoursePdf> {
}
