package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.exam.ImportResultVO;
import com.gmp.dto.exam.QuestionExcelVO;
import com.gmp.dto.exam.QuestionSaveDTO;
import com.gmp.dto.exam.QuestionVO;
import com.gmp.entity.TrQuestion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 题库Service接口
 */
public interface TrQuestionService extends IService<TrQuestion> {

    /**
     * 分页查询题目
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param keyword    题目内容关键词（模糊）
     * @param type       题型（single/multiple/judge/fill/short_answer）
     * @param difficulty 难度（easy/medium/hard）
     * @param category   课程分类编码
     * @return 分页结果
     */
    Page<QuestionVO> pageQuestions(Integer pageNum, Integer pageSize, String keyword,
                                   String type, String difficulty, String category);

    /**
     * 新增题目
     */
    void addQuestion(QuestionSaveDTO dto);

    /**
     * 修改题目
     */
    void updateQuestion(QuestionSaveDTO dto);

    /**
     * Excel 批量导入题目
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果（成功/失败条数与失败原因）
     */
    ImportResultVO importQuestions(MultipartFile file);

    /**
     * 查询全部题目并转换为 Excel 导出模型
     */
    List<QuestionExcelVO> listForExport();

    /**
     * 生成导入模板数据（含一行示例）
     */
    List<QuestionExcelVO> templateData();
}
