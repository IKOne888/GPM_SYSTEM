package com.gmp.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gmp.common.BusinessException;
import com.gmp.common.ErrorCode;
import com.gmp.common.Result;
import com.gmp.config.MinioConfig;
import com.gmp.dto.pdf.CoursePdfVO;
import com.gmp.entity.TrCourse;
import com.gmp.entity.TrCoursePdf;
import com.gmp.mapper.TrCourseMapper;
import com.gmp.service.TrCoursePdfService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PDF 课件管理 Controller
 * <p>
 * 路径前缀 /training/course-pdfs，与前端 views/training/pdf/index.vue 的接口契约保持一致。
 * 功能：课程 PDF 课件的上传、分页查询、在线预览、启用/停用、删除。
 * 安全要求：
 * 1. 上传前校验真实文件类型（Magic Bytes 必须为 %PDF），防止扩展名伪造；
 * 2. 校验课程存在性；
 * 3. 删除时联动删除 MinIO 对象，避免产生孤儿文件；
 * 4. 接口权限由 SaTokenConfig 限制为管理员角色。
 */
@Slf4j
@Api(tags = "PDF课件管理")
@RestController
@RequestMapping("/training/course-pdfs")
@RequiredArgsConstructor
@Validated
public class TrCoursePdfController {

    /** PDF 文件头标识：%PDF（0x25 0x50 0x44 0x46） */
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    /** PDF 课件大小上限（单位字节，默认 50MB，可通过配置覆盖） */
    @Value("${course.pdf-max-size:52428800}")
    private long maxPdfSize;

    /** PDF 课件 Service */
    private final TrCoursePdfService coursePdfService;
    /** MinIO 客户端（文件上传/删除/预签名） */
    private final MinioClient minioClient;
    /** MinIO 配置（读取桶名称） */
    private final MinioConfig minioConfig;
    /** 课程 Mapper（校验课程存在性） */
    private final TrCourseMapper courseMapper;

    /**
     * 分页查询 PDF 课件
     */
    @ApiOperation("分页查询PDF课件")
    @GetMapping
    public Result<Page<CoursePdfVO>> page(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10")
            @Min(value = 1, message = "每页条数最小为1")
            @Max(value = 100, message = "每页条数最大为100") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(coursePdfService.pagePdfs(page, size, courseId, keyword, status));
    }

    /**
     * 上传 PDF 课件
     */
    @ApiOperation("上传PDF课件")
    @PostMapping("/upload")
    @Transactional(rollbackFor = Exception.class)
    public Result<TrCoursePdf> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") @NotNull(message = "请选择所属课程") Long courseId,
            @RequestParam(value = "name", required = false) String name) {

        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        // 2. 校验文件大小
        if (file.getSize() > maxPdfSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "课件大小超出上限（" + maxPdfSize / 1024 / 1024 + "MB）");
        }
        // 3. 校验课程存在性
        TrCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在，无法上传课件");
        }

        String fileName = file.getOriginalFilename();
        // 4. 校验扩展名必须为 pdf
        if (!"pdf".equalsIgnoreCase(getFileExtension(fileName))) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "PDF课件仅支持pdf格式文件");
        }

        try {
            // 5. 校验文件真实类型（读取文件头 Magic Bytes），防止把其他文件改名为 .pdf 上传
            validatePdfMagicBytes(file);

            // 6. 生成唯一文件名与 MinIO 对象路径（按 年/月 归档）
            String newFileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
            LocalDateTime now = LocalDateTime.now();
            String objectName = "course-pdfs/" + now.getYear() + "/" + now.getMonthValue() + "/" + newFileName;

            // 7. 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType("application/pdf")
                        .build());
            }

            // 8. 保存元数据到数据库，新上传课件默认启用
            TrCoursePdf entity = new TrCoursePdf();
            entity.setCourseId(courseId);
            entity.setName(name != null && !name.trim().isEmpty() ? name : fileName);
            entity.setFileName(fileName);
            entity.setFilePath(objectName);
            entity.setFileSize(file.getSize());
            entity.setUploadUserId(StpUtil.getLoginIdAsLong());
            entity.setUploadTime(now);
            entity.setStatus(1);

            coursePdfService.save(entity);

            return Result.success(entity);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF课件上传失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "PDF课件上传失败，请稍后重试");
        }
    }

    /**
     * 获取课件预览地址（MinIO 预签名 URL）
     */
    @ApiOperation("获取PDF课件预览地址")
    @GetMapping("/{id}/url")
    public Result<String> getPreviewUrl(@PathVariable @NotNull(message = "课件ID不能为空") Long id) {
        TrCoursePdf pdf = coursePdfService.getById(id);
        if (pdf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PDF课件不存在");
        }
        try {
            // 生成预签名URL（有效期7天），前端据此在新窗口打开预览
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(pdf.getFilePath())
                            .method(Method.GET)
                            .expiry(7 * 24 * 60 * 60)
                            .build());
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取PDF课件预览地址失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取预览地址失败");
        }
    }

    /**
     * 启用 / 停用课件
     */
    @ApiOperation("启用/停用PDF课件")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable @NotNull(message = "课件ID不能为空") Long id,
                                     @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        coursePdfService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 删除 PDF 课件（逻辑删除 + 联动删除 MinIO 对象）
     */
    @ApiOperation("删除PDF课件")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable @NotNull(message = "课件ID不能为空") Long id) {
        TrCoursePdf pdf = coursePdfService.getById(id);
        if (pdf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PDF课件不存在");
        }
        // 先删除 MinIO 对象，再删除数据库记录（异常时事务回滚，保证一致性）
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(pdf.getFilePath())
                    .build());
        } catch (Exception e) {
            log.error("删除MinIO PDF课件失败：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "删除课件文件失败");
        }
        coursePdfService.removeById(id);
        return Result.success();
    }

    /**
     * 提取文件扩展名（小写）
     *
     * @param fileName 原始文件名
     * @return 小写扩展名，无扩展名时返回空串
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 校验 PDF 文件头 Magic Bytes，防止伪造扩展名
     * <p>
     * 标准 PDF 文件以「%PDF」四个字节开头（0x25 0x50 0x44 0x46）。
     *
     * @param file 上传文件
     */
    private void validatePdfMagicBytes(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = inputStream.read(header);
            // 文件过小，无法识别格式
            if (read < 4) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容不完整，无法识别格式");
            }
            // 逐字节比对 PDF 文件头
            for (int i = 0; i < PDF_MAGIC.length; i++) {
                if (header[i] != PDF_MAGIC[i]) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件内容与声明格式不符，已拒绝上传");
                }
            }
        }
    }
}
