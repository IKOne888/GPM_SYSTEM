package com.gmp.dto.study;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训档案 / 证书 VO
 * <p>
 * 按课程「再培训周期 retrainPeriod」计算证书有效期：
 * 有效期至 = 最近一次通过考试时间 + retrainPeriod 天；
 * 状态：valid-有效、expiring-临期（≤30天）、expired-已过期、not_pass-未通过考试。
 */
@Data
@ApiModel("培训档案/证书")
public class CertificateVO {

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("课程编码")
    private String courseCode;

    @ApiModelProperty("SOP编号")
    private String sopCode;

    @ApiModelProperty("SOP版本")
    private String sopVersion;

    @ApiModelProperty("培训学时")
    private BigDecimal trainingHours;

    @ApiModelProperty("再培训周期（天）")
    private Integer retrainPeriod;

    @ApiModelProperty("最近一次通过时间")
    private LocalDateTime lastPassTime;

    @ApiModelProperty("最近一次通过成绩")
    private Integer examScore;

    @ApiModelProperty("有效期至")
    private LocalDateTime validUntil;

    @ApiModelProperty("距到期剩余天数（已过期时为负数）")
    private Long remainDays;

    @ApiModelProperty("证书状态：valid-有效 expiring-临期 expired-已过期 not_pass-未通过")
    private String status;
}
