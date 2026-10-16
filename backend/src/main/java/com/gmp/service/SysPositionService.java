package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.PositionMatrixVO;
import com.gmp.entity.SysPosition;

import java.util.List;

/**
 * 岗位Service接口
 */
public interface SysPositionService extends IService<SysPosition> {

    /**
     * 查询岗位列表（含所属部门名称）
     */
    List<SysPosition> listPositions(String keyword);

    /**
     * 分页查询岗位（含所属部门名称）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param name     岗位名称模糊查询（可选）
     * @param status   状态查询（可选）
     */
    Page<SysPosition> pagePositions(Integer pageNum, Integer pageSize, String name, Integer status);

    /**
     * 查询岗位培训矩阵（列出全部启用课程及其在该岗位下的培训要求）
     *
     * @param positionId 岗位ID
     */
    List<PositionMatrixVO> getTrainingMatrix(Long positionId);

    /**
     * 保存岗位培训矩阵（全量覆盖）
     *
     * @param positionId 岗位ID
     * @param rows       矩阵行
     */
    void saveTrainingMatrix(Long positionId, List<PositionMatrixVO> rows);
}
