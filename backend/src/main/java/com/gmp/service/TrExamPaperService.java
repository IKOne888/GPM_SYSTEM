package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.exam.PaperSaveDTO;
import com.gmp.dto.exam.PaperVO;
import com.gmp.entity.TrExamPaper;

/**
 * 试卷Service接口
 */
public interface TrExamPaperService extends IService<TrExamPaper> {

    /**
     * 分页查询试卷
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param name     试卷名称（模糊）
     * @param status   状态（draft/published/archived）
     * @return 分页结果
     */
    Page<PaperVO> pagePapers(Integer pageNum, Integer pageSize, String name, String status);

    /**
     * 新增试卷
     */
    void addPaper(PaperSaveDTO dto);

    /**
     * 修改试卷
     */
    void updatePaper(PaperSaveDTO dto);
}
