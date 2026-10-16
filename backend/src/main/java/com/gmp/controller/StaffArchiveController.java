package com.gmp.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.dto.archive.ArchiveOverviewVO;
import com.gmp.dto.study.CertificateVO;
import com.gmp.dto.study.ExamRecordVO;
import com.gmp.dto.study.StudyRecordVO;
import com.gmp.entity.SysDepartment;
import com.gmp.entity.SysPosition;
import com.gmp.entity.SysUser;
import com.gmp.entity.TrExamRecord;
import com.gmp.entity.TrVideoStudyRecord;
import com.gmp.mapper.SysUserMapper;
import com.gmp.mapper.TrExamRecordMapper;
import com.gmp.mapper.TrVideoStudyRecordMapper;
import com.gmp.service.StudyService;
import com.gmp.service.SysDepartmentService;
import com.gmp.service.SysPositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 员工培训档案（管理员视角）
 * <p>
 * 管理端查看指定员工的学习记录、考试记录、培训证书，并汇总统计卡片；
 * 复用 StudyService 带 userId 参数的重载方法，保证与学员端查询逻辑一致。
 * <p>
 * 权限：SUPER_ADMIN / QA_MANAGER / TRAINING_ADMIN / DEPT_LEADER（SaTokenConfig 统一拦截）。
 */
@RestController
@RequestMapping("/system/archive")
@RequiredArgsConstructor
@Api(tags = "员工培训档案")
public class StaffArchiveController {

    /** 学员端服务（复用带 userId 参数的重载方法） */
    private final StudyService studyService;

    /** 用户 Mapper（查员工基础信息） */
    private final SysUserMapper userMapper;

    /** 部门 Service（查部门名称） */
    private final SysDepartmentService departmentService;

    /** 岗位 Service（查岗位名称） */
    private final SysPositionService positionService;

    /** 学习记录 Mapper（聚合统计） */
    private final TrVideoStudyRecordMapper studyMapper;

    /** 考试记录 Mapper（聚合统计） */
    private final TrExamRecordMapper examMapper;

    /**
     * 员工档案概览（基础信息 + 学习/考试/证书统计）
     */
    @GetMapping("/{userId}/overview")
    @ApiOperation("员工档案概览")
    public Result<ArchiveOverviewVO> overview(@PathVariable Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "员工不存在");
        }

        ArchiveOverviewVO vo = new ArchiveOverviewVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setUsername(user.getUsername());
        vo.setLastLoginTime(user.getLastLoginTime());

        // 部门 / 岗位名称（通过 Service 查询，避免 XML 改造）
        if (user.getDepartmentId() != null) {
            SysDepartment dept = departmentService.getById(user.getDepartmentId());
            if (dept != null) {
                vo.setDepartmentName(dept.getName());
            }
        }
        if (user.getPositionId() != null) {
            SysPosition pos = positionService.getById(user.getPositionId());
            if (pos != null) {
                vo.setPositionName(pos.getName());
            }
        }

        // 学习记录统计
        Long studyCount = studyMapper.selectCount(new LambdaQueryWrapper<TrVideoStudyRecord>()
                .eq(TrVideoStudyRecord::getUserId, userId));
        vo.setStudyRecordCount(studyCount);
        // 累计学习时长（秒转小时，保留1位）
        Integer totalSeconds = studyMapper.selectTotalStudyDuration(userId);
        long safeSeconds = totalSeconds == null ? 0 : totalSeconds;
        BigDecimal hours = BigDecimal.valueOf(safeSeconds)
                .divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP);
        vo.setTotalStudyHours(hours);

        // 考试统计（排除进行中的）
        Long examCount = examMapper.selectCount(new LambdaQueryWrapper<TrExamRecord>()
                .eq(TrExamRecord::getUserId, userId)
                .ne(TrExamRecord::getStatus, 0));
        vo.setExamCount(examCount);
        Long passedCount = examMapper.selectCount(new LambdaQueryWrapper<TrExamRecord>()
                .eq(TrExamRecord::getUserId, userId)
                .ne(TrExamRecord::getStatus, 0)
                .eq(TrExamRecord::getPass, 1));
        vo.setPassedExamCount(passedCount);
        Integer avg = examMapper.selectAvgPassScore(userId);
        vo.setAvgPassScore(avg == null ? null : BigDecimal.valueOf(avg));

        // 证书统计（调用同一套 Service 逻辑保证状态计算一致）
        List<CertificateVO> certs = studyService.certificates(userId);
        long valid = certs.stream().filter(c -> "valid".equals(c.getStatus())).count();
        long expiring = certs.stream().filter(c -> "expiring".equals(c.getStatus())).count();
        long expired = certs.stream().filter(c -> "expired".equals(c.getStatus())).count();
        vo.setValidCertCount(valid);
        vo.setExpiringCertCount(expiring);
        vo.setExpiredCertCount(expired);

        return Result.success(vo);
    }

    /**
     * 员工学习记录分页
     */
    @GetMapping("/{userId}/study-records")
    @ApiOperation("员工学习记录分页")
    public Result<Page<StudyRecordVO>> studyRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageStudyRecords(userId, page, size, keyword));
    }

    /**
     * 员工考试记录分页
     */
    @GetMapping("/{userId}/exam-records")
    @ApiOperation("员工考试记录分页")
    public Result<Page<ExamRecordVO>> examRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(studyService.pageExamRecords(userId, page, size, keyword));
    }

    /**
     * 员工培训档案 / 证书列表
     */
    @GetMapping("/{userId}/certificates")
    @ApiOperation("员工培训档案/证书")
    public Result<List<CertificateVO>> certificates(@PathVariable Long userId) {
        return Result.success(studyService.certificates(userId));
    }
}
