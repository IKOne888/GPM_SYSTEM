package com.gmp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.dto.exam.ImportResultVO;
import com.gmp.dto.exam.QuestionExcelVO;
import com.gmp.dto.exam.QuestionOptionDTO;
import com.gmp.dto.exam.QuestionSaveDTO;
import com.gmp.dto.exam.QuestionVO;
import com.gmp.entity.TrQuestion;
import com.gmp.mapper.TrQuestionMapper;
import com.gmp.service.TrQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库Service实现类
 * <p>
 * 负责前端字符串枚举（type/difficulty）与数据库数字枚举的相互转换，
 * 以及选项 JSON 的序列化/反序列化。
 */
@Slf4j
@Service
public class TrQuestionServiceImpl extends ServiceImpl<TrQuestionMapper, TrQuestion> implements TrQuestionService {

    /** 前端题型字符串 → 数据库题型数字 */
    private static final Map<String, Integer> TYPE_TO_INT = new HashMap<>();
    /** 数据库题型数字 → 前端题型字符串 */
    private static final Map<Integer, String> INT_TO_TYPE = new HashMap<>();
    /** 前端难度字符串 → 数据库难度数字 */
    private static final Map<String, Integer> DIFFICULTY_TO_INT = new HashMap<>();
    /** 数据库难度数字 → 前端难度字符串 */
    private static final Map<Integer, String> INT_TO_DIFFICULTY = new HashMap<>();
    /** Excel 题型中文名 → 数据库题型数字 */
    private static final Map<String, Integer> EXCEL_TYPE_TO_INT = new HashMap<>();

    static {
        // 题型映射（前端 code）
        TYPE_TO_INT.put("single", 1);
        TYPE_TO_INT.put("multiple", 2);
        TYPE_TO_INT.put("judge", 3);
        TYPE_TO_INT.put("fill", 4);
        TYPE_TO_INT.put("short_answer", 5);
        INT_TO_TYPE.put(1, "single");
        INT_TO_TYPE.put(2, "multiple");
        INT_TO_TYPE.put(3, "judge");
        INT_TO_TYPE.put(4, "fill");
        INT_TO_TYPE.put(5, "short_answer");

        // 难度映射
        DIFFICULTY_TO_INT.put("easy", 1);
        DIFFICULTY_TO_INT.put("medium", 2);
        DIFFICULTY_TO_INT.put("hard", 3);
        INT_TO_DIFFICULTY.put(1, "easy");
        INT_TO_DIFFICULTY.put(2, "medium");
        INT_TO_DIFFICULTY.put(3, "hard");

        // Excel 导入时题型支持中文名与英文 code 两种写法
        EXCEL_TYPE_TO_INT.put("单选题", 1);
        EXCEL_TYPE_TO_INT.put("多选题", 2);
        EXCEL_TYPE_TO_INT.put("判断题", 3);
        EXCEL_TYPE_TO_INT.put("填空题", 4);
        EXCEL_TYPE_TO_INT.put("简答题", 5);
        EXCEL_TYPE_TO_INT.put("single", 1);
        EXCEL_TYPE_TO_INT.put("multiple", 2);
        EXCEL_TYPE_TO_INT.put("judge", 3);
        EXCEL_TYPE_TO_INT.put("fill", 4);
        EXCEL_TYPE_TO_INT.put("short_answer", 5);
    }

    @Override
    public Page<QuestionVO> pageQuestions(Integer pageNum, Integer pageSize, String keyword,
                                          String type, String difficulty, String category) {
        Page<TrQuestion> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrQuestion> wrapper = new LambdaQueryWrapper<>();
        // 题目内容模糊搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(TrQuestion::getContent, keyword);
        }
        // 题型过滤（字符串 → 数字）
        Integer typeInt = parseType(type);
        if (typeInt != null) {
            wrapper.eq(TrQuestion::getQuestionType, typeInt);
        }
        // 难度过滤
        Integer diffInt = parseDifficulty(difficulty);
        if (diffInt != null) {
            wrapper.eq(TrQuestion::getDifficulty, diffInt);
        }
        // 分类过滤
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(TrQuestion::getCategory, category);
        }
        wrapper.orderByDesc(TrQuestion::getCreateTime);

        Page<TrQuestion> entityPage = baseMapper.selectPage(page, wrapper);

        // 转换为 VO（含枚举反向映射、选项 JSON 解析）
        Page<QuestionVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestion(QuestionSaveDTO dto) {
        TrQuestion question = buildEntity(dto);
        question.setStatus(1);
        baseMapper.insert(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(QuestionSaveDTO dto) {
        // 校验题目存在
        if (baseMapper.selectById(dto.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        TrQuestion question = buildEntity(dto);
        question.setId(dto.getId());
        baseMapper.updateById(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importQuestions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入文件不能为空");
        }
        ImportResultVO result = new ImportResultVO();
        List<QuestionExcelVO> rows;
        try (InputStream in = file.getInputStream()) {
            // 使用 EasyExcel 同步读取全部行
            rows = EasyExcel.read(in).head(QuestionExcelVO.class).sheet().doReadSync();
        } catch (Exception e) {
            log.error("题库导入解析失败", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Excel 解析失败，请确认文件格式与模板一致");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Excel 中没有可导入的数据");
        }

        // 逐行解析并入库，单行失败不影响其它行
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 2; // Excel 首行为表头，数据从第 2 行开始
            QuestionExcelVO row = rows.get(i);
            try {
                TrQuestion question = convertExcelRow(row);
                baseMapper.insert(question);
                result.addSuccess();
            } catch (Exception e) {
                result.addFail("第 " + rowNo + " 行：" + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public List<QuestionExcelVO> listForExport() {
        List<TrQuestion> list = baseMapper.selectList(
                new LambdaQueryWrapper<TrQuestion>().orderByDesc(TrQuestion::getCreateTime));
        return list.stream().map(this::toExcelVO).collect(Collectors.toList());
    }

    @Override
    public List<QuestionExcelVO> templateData() {
        // 模板提供一行示例，便于用户理解各列填写规范
        QuestionExcelVO sample = new QuestionExcelVO();
        sample.setType("单选题");
        sample.setContent("GMP 的中文全称是什么？");
        sample.setOptionA("药品生产质量管理规范");
        sample.setOptionB("药品经营质量管理规范");
        sample.setOptionC("药品临床试验管理规范");
        sample.setOptionD("药品注册管理规范");
        sample.setAnswer("A");
        sample.setDifficulty("简单");
        sample.setCategory("gmp_basic");
        sample.setScore(5);
        sample.setAnalysis("GMP 即 Good Manufacturing Practice，药品生产质量管理规范。");
        return Collections.singletonList(sample);
    }

    /**
     * 将保存 DTO 转换为实体（含枚举映射与选项 JSON 构建）
     *
     * @param dto 保存参数
     * @return 题库实体
     */
    private TrQuestion buildEntity(QuestionSaveDTO dto) {
        Integer typeInt = parseType(dto.getType());
        if (typeInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "题型不合法");
        }
        Integer diffInt = parseDifficulty(dto.getDifficulty());
        if (diffInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "难度不合法");
        }

        TrQuestion question = new TrQuestion();
        question.setQuestionType(typeInt);
        question.setContent(dto.getContent());
        question.setDifficulty(diffInt);
        question.setCategory(dto.getCategory());
        question.setScore(dto.getScore());
        question.setAnalysis(dto.getAnalysis());

        // 单选/多选：由选项的 isAnswer 推导正确答案（字母），选项以 JSON 存储
        if (typeInt == 1 || typeInt == 2) {
            List<QuestionOptionDTO> options = dto.getOptions();
            if (options == null || options.size() < 2) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "单选题/多选题至少需要 2 个选项");
            }
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                if (Boolean.TRUE.equals(options.get(i).getIsAnswer())) {
                    answer.append((char) ('A' + i));
                }
            }
            if (answer.length() == 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请勾选正确答案");
            }
            question.setOptions(JSONUtil.toJsonStr(options));
            question.setAnswer(answer.toString());
        } else {
            // 判断/填空/简答：无选项，答案直接取文本
            if (StrUtil.isBlank(dto.getAnswer())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写正确答案");
            }
            question.setOptions(null);
            question.setAnswer(dto.getAnswer());
        }
        return question;
    }

    /**
     * 将 Excel 行转换为实体（导入时使用）
     *
     * @param row Excel 行数据
     * @return 题库实体
     */
    private TrQuestion convertExcelRow(QuestionExcelVO row) {
        if (row == null || StrUtil.isBlank(row.getContent())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "题目内容为空");
        }
        Integer typeInt = EXCEL_TYPE_TO_INT.get(StrUtil.trimToEmpty(row.getType()));
        if (typeInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "题型不合法（应为 单选题/多选题/判断题/填空题/简答题）");
        }
        String diffText = StrUtil.trimToEmpty(row.getDifficulty());
        Integer diffInt = EXCEL_DIFFICULTY(diffText);

        TrQuestion question = new TrQuestion();
        question.setQuestionType(typeInt);
        question.setContent(row.getContent().trim());
        question.setDifficulty(diffInt);
        question.setCategory(StrUtil.trimToEmpty(row.getCategory()));
        question.setScore(row.getScore() == null || row.getScore() <= 0 ? 5 : row.getScore());
        question.setAnalysis(row.getAnalysis());
        question.setStatus(1);

        if (typeInt == 1 || typeInt == 2) {
            // 由 A~D 列组装选项，并依据答案字母标记正确项
            String answer = StrUtil.trimToEmpty(row.getAnswer()).toUpperCase();
            List<QuestionOptionDTO> options = new ArrayList<>();
            String[] texts = {row.getOptionA(), row.getOptionB(), row.getOptionC(), row.getOptionD()};
            for (int i = 0; i < texts.length; i++) {
                if (StrUtil.isNotBlank(texts[i])) {
                    QuestionOptionDTO opt = new QuestionOptionDTO();
                    opt.setText(texts[i].trim());
                    opt.setIsAnswer(answer.contains(String.valueOf((char) ('A' + i))));
                    options.add(opt);
                }
            }
            if (options.size() < 2) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "选项不足 2 个");
            }
            if (answer.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "正确答案为空");
            }
            question.setOptions(JSONUtil.toJsonStr(options));
            question.setAnswer(answer);
        } else {
            if (StrUtil.isBlank(row.getAnswer())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "正确答案为空");
            }
            question.setOptions(null);
            question.setAnswer(row.getAnswer().trim());
        }
        return question;
    }

    /**
     * Excel 难度文本转数字（兼容 简单/中等/困难 与 easy/medium/hard）
     */
    private Integer EXCEL_DIFFICULTY(String text) {
        if (StrUtil.isBlank(text)) {
            return 2; // 未填写时默认中等
        }
        switch (text) {
            case "简单":
                return 1;
            case "中等":
                return 2;
            case "困难":
                return 3;
            default:
                Integer v = DIFFICULTY_TO_INT.get(text);
                return v == null ? 2 : v;
        }
    }

    /**
     * 实体转 VO（枚举反向映射 + 选项 JSON 解析）
     */
    private QuestionVO toVO(TrQuestion question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setCourseId(question.getCourseId());
        vo.setType(INT_TO_TYPE.getOrDefault(question.getQuestionType(), "single"));
        vo.setContent(question.getContent());
        vo.setDifficulty(INT_TO_DIFFICULTY.getOrDefault(question.getDifficulty(), "medium"));
        vo.setCategory(question.getCategory());
        vo.setScore(question.getScore());
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setStatus(question.getStatus());
        vo.setCreateTime(question.getCreateTime());
        // 解析选项 JSON 为数组返回
        if (StrUtil.isNotBlank(question.getOptions())) {
            vo.setOptions(JSONUtil.toList(question.getOptions(), QuestionOptionDTO.class));
        } else {
            vo.setOptions(new ArrayList<>());
        }
        return vo;
    }

    /**
     * 实体转 Excel 导出模型
     */
    private QuestionExcelVO toExcelVO(TrQuestion question) {
        QuestionExcelVO vo = new QuestionExcelVO();
        vo.setType(excelTypeName(question.getQuestionType()));
        vo.setContent(question.getContent());
        // 选项依次写入 A~D 列
        if (StrUtil.isNotBlank(question.getOptions())) {
            List<QuestionOptionDTO> options = JSONUtil.toList(question.getOptions(), QuestionOptionDTO.class);
            String[] target = new String[4];
            for (int i = 0; i < options.size() && i < 4; i++) {
                target[i] = options.get(i).getText();
            }
            vo.setOptionA(target[0]);
            vo.setOptionB(target[1]);
            vo.setOptionC(target[2]);
            vo.setOptionD(target[3]);
        }
        vo.setAnswer(question.getAnswer());
        vo.setDifficulty(excelDifficultyName(question.getDifficulty()));
        vo.setCategory(question.getCategory());
        vo.setScore(question.getScore());
        vo.setAnalysis(question.getAnalysis());
        return vo;
    }

    /**
     * 题型数字转 Excel 中文名
     */
    private String excelTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case 1:
                return "单选题";
            case 2:
                return "多选题";
            case 3:
                return "判断题";
            case 4:
                return "填空题";
            case 5:
                return "简答题";
            default:
                return "";
        }
    }

    /**
     * 难度数字转 Excel 中文名
     */
    private String excelDifficultyName(Integer difficulty) {
        if (difficulty == null) {
            return "";
        }
        switch (difficulty) {
            case 1:
                return "简单";
            case 2:
                return "中等";
            case 3:
                return "困难";
            default:
                return "";
        }
    }

    /**
     * 解析题型字符串（空返回 null 表示不过滤）
     */
    private Integer parseType(String type) {
        if (StrUtil.isBlank(type)) {
            return null;
        }
        Integer value = TYPE_TO_INT.get(type);
        if (value == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "题型不合法");
        }
        return value;
    }

    /**
     * 解析难度字符串（空返回 null 表示不过滤）
     */
    private Integer parseDifficulty(String difficulty) {
        if (StrUtil.isBlank(difficulty)) {
            return null;
        }
        Integer value = DIFFICULTY_TO_INT.get(difficulty);
        if (value == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "难度不合法");
        }
        return value;
    }
}
