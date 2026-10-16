package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.dto.UserInfoVO;
import com.gmp.dto.study.AnalysisOverviewVO;
import com.gmp.dto.study.CalendarDayVO;
import com.gmp.dto.study.CertificateVO;
import com.gmp.dto.study.CourseFileVO;
import com.gmp.dto.study.ExamInfoVO;
import com.gmp.dto.study.ExamRecordVO;
import com.gmp.dto.study.ExamResultVO;
import com.gmp.dto.study.ExamStartVO;
import com.gmp.dto.study.ExamSubmitDTO;
import com.gmp.dto.study.MaterialVO;
import com.gmp.dto.study.PasswordUpdateDTO;
import com.gmp.dto.study.ProfileUpdateDTO;
import com.gmp.dto.study.ProgressReportDTO;
import com.gmp.dto.study.StudyCourseVO;
import com.gmp.dto.study.StudyRecordVO;
import com.gmp.dto.study.StudyStatsVO;
import com.gmp.dto.study.StudyTaskVO;
import com.gmp.dto.study.TaskDetailVO;
import com.gmp.dto.study.VideoStudyVO;
import com.gmp.dto.study.WrongPracticeDTO;
import com.gmp.dto.study.WrongPracticeResultVO;
import com.gmp.dto.study.WrongQuestionStatsVO;
import com.gmp.dto.study.WrongQuestionVO;
import com.gmp.entity.SysMessage;

import java.time.LocalDate;
import java.util.List;

/**
 * 学员端（用户端）业务 Service 接口
 * <p>
 * 统一承载「我的任务 / 在线学习 / 在线考试 / 个人中心与培训档案」四大模块的业务逻辑。
 * <p>
 * 安全约定：所有方法内部均以当前登录用户 {@code StpUtil.getLoginIdAsLong()} 作为数据边界，
 * 严禁越权读取或修改他人数据。
 */
public interface StudyService {

    // ==================== 模块一：我的任务 ====================

    /**
     * 分页查询当前学员的培训任务
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param status   状态筛选：not_started/studying/waiting_exam/completed/overdue（可空）
     * @param keyword  任务名称模糊关键字（可空）
     * @return 任务分页
     */
    Page<StudyTaskVO> pageTasks(Integer pageNum, Integer pageSize, String status, String keyword);

    /**
     * 查询当前学员各状态任务数量统计
     *
     * @return 统计结果
     */
    StudyStatsVO taskStats();

    /**
     * 查询任务详情（含课程、视频、资料与考试信息）
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    TaskDetailVO taskDetail(Long taskId);

    /**
     * 查询任务下的视频课件列表（含本人学习进度）
     *
     * @param taskId 任务ID
     * @return 视频列表
     */
    List<VideoStudyVO> taskVideos(Long taskId);

    // ==================== 模块二：在线学习 ====================

    /**
     * 查询单个视频的学习记录（用于断点续播）
     *
     * @param taskId  任务ID
     * @param videoId 视频ID
     * @return 视频学习情况（无记录时返回默认值）
     */
    VideoStudyVO videoRecord(Long taskId, Long videoId);

    /**
     * 上报视频学习进度（节流由前端控制，后端保证幂等与越权校验）
     *
     * @param dto 上报参数
     */
    void reportProgress(ProgressReportDTO dto);

    /**
     * 查询课程资料附件列表
     *
     * @param courseId 课程ID
     * @return 资料列表
     */
    List<CourseFileVO> courseFiles(Long courseId);

    // ==================== 模块三：在线考试 ====================

    /**
     * 查询任务的考试信息（试卷参数、剩余次数、上次成绩）
     *
     * @param taskId 任务ID
     * @return 考试信息
     */
    ExamInfoVO examInfo(Long taskId);

    /**
     * 开始考试（创建考试记录并固化本次题目快照）
     *
     * @param taskId 任务ID
     * @return 考试进行中数据（题目 + 截止时间）
     */
    ExamStartVO startExam(Long taskId);

    /**
     * 取回考试进行中的数据（刷新页面后恢复答题状态）
     *
     * @param recordId 考试记录ID
     * @return 考试进行中数据
     */
    ExamStartVO getExamRecord(Long recordId);

    /**
     * 交卷并自动判分
     *
     * @param recordId 考试记录ID
     * @param dto      作答内容
     * @return 成绩单（含逐题解析）
     */
    ExamResultVO submitExam(Long recordId, ExamSubmitDTO dto);

    /**
     * 查询考试成绩单与逐题解析
     *
     * @param recordId 考试记录ID
     * @return 成绩单
     */
    ExamResultVO examResult(Long recordId);

    // ==================== 模块四：个人中心与培训档案 ====================

    /**
     * 查询当前学员个人信息（脱敏）
     *
     * @return 用户信息
     */
    UserInfoVO profile();

    /**
     * 修改个人资料（姓名/手机/邮箱）
     *
     * @param dto 修改参数
     */
    void updateProfile(ProfileUpdateDTO dto);

    /**
     * 修改登录密码
     *
     * @param dto 修改参数
     */
    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 分页查询学习记录
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  视频名称关键字（可空）
     * @return 学习记录分页
     */
    Page<StudyRecordVO> pageStudyRecords(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 【管理员视角】按指定 userId 分页查询学习记录
     *
     * @param userId   目标员工ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  视频名称关键字（可空）
     * @return 学习记录分页
     */
    Page<StudyRecordVO> pageStudyRecords(Long userId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 分页查询考试成绩单
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  试卷/任务名称关键字（可空）
     * @return 成绩单分页
     */
    Page<ExamRecordVO> pageExamRecords(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 【管理员视角】按指定 userId 分页查询考试成绩单
     *
     * @param userId   目标员工ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  试卷/任务名称关键字（可空）
     * @return 成绩单分页
     */
    Page<ExamRecordVO> pageExamRecords(Long userId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 查询培训档案 / 证书（按再培训周期计算有效期）
     *
     * @return 证书列表
     */
    List<CertificateVO> certificates();

    /**
     * 【管理员视角】按指定 userId 查询培训档案 / 证书
     *
     * @param userId 目标员工ID
     * @return 证书列表
     */
    List<CertificateVO> certificates(Long userId);

    /**
     * 分页查询消息通知
     *
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @param isRead      是否已读筛选（可空：0-未读 1-已读）
     * @param messageType 消息类型筛选（可空：1-任务通知 2-到期提醒 3-补考提醒 4-系统通知）
     * @return 消息分页
     */
    Page<SysMessage> pageMessages(Integer pageNum, Integer pageSize, Integer isRead, Integer messageType);

    /**
     * 查询当前学员未读消息数量
     *
     * @return 未读数
     */
    Long unreadCount();

    /**
     * 将指定消息标记为已读
     *
     * @param id 消息ID
     */
    void readMessage(Long id);

    /**
     * 将当前学员全部消息标记为已读
     */
    void readAllMessages();

    // ==================== 模块五：学习看板 ====================

    /**
     * 查询学习看板聚合数据（指标卡 + 三张图表）
     *
     * @return 看板数据
     */
    AnalysisOverviewVO analysisOverview();

    // ==================== 模块六：课程中心（自主选学） ====================

    /**
     * 分页查询推荐课程列表
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param keyword    课程名称关键字（可空）
     * @param categoryId 课程分类ID（可空）
     * @param enrolled   报名状态筛选（可空：true-已报名 false-未报名）
     * @return 课程分页
     */
    Page<StudyCourseVO> pageCourses(Integer pageNum, Integer pageSize, String keyword, Long categoryId, Boolean enrolled);

    /**
     * 查询课程详情
     *
     * @param courseId 课程ID
     * @return 课程详情
     */
    StudyCourseVO courseDetail(Long courseId);

    /**
     * 自主报名课程（生成自主学习任务）
     *
     * @param courseId 课程ID
     * @return 新建任务ID
     */
    Long enrollCourse(Long courseId);

    // ==================== 模块七：培训日历 ====================

    /**
     * 查询指定日期区间内的任务截止与考试安排事件
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 有事件的日期列表
     */
    List<CalendarDayVO> calendar(LocalDate startDate, LocalDate endDate);

    // ==================== 模块八：错题本与题库练习 ====================

    /**
     * 分页查询错题列表
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param courseId     课程ID筛选（可空）
     * @param questionType 题型筛选（可空）
     * @param mastered     掌握状态筛选（可空，默认只看未掌握）
     * @return 错题分页
     */
    Page<WrongQuestionVO> pageWrongQuestions(Integer pageNum, Integer pageSize, Long courseId,
                                            Integer questionType, Boolean mastered);

    /**
     * 查询错题统计（总数 + 按题型/按课程分布）
     *
     * @return 错题统计
     */
    WrongQuestionStatsVO wrongQuestionStats();

    /**
     * 错题重做（单题即时判分，答对自动移出错题本）
     *
     * @param dto 重做参数
     * @return 判分结果
     */
    WrongPracticeResultVO practiceWrongQuestion(WrongPracticeDTO dto);

    /**
     * 手动移除错题（标记为已掌握）
     *
     * @param id 错题记录ID
     */
    void removeWrongQuestion(Long id);

    // ==================== 模块九：资料中心 ====================

    /**
     * 分页查询本人有权访问的课程资料
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param courseId 课程ID筛选（可空）
     * @param fileType 文件类型筛选（可空）
     * @param keyword  资料名称关键字（可空）
     * @return 资料分页
     */
    Page<MaterialVO> pageMaterials(Integer pageNum, Integer pageSize, Long courseId, String fileType, String keyword);
}
