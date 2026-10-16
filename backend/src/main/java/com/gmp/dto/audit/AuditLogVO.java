package com.gmp.dto.audit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 审计日志列表返回 VO
 * <p>
 * 字段名对齐前端 views/audit/log/index.vue：
 * action ← operationType（大写转小写），operator ← realName 优先，operatorIp ← ip，
 * duration ← executeTime，operateTime ← operationTime，responseResult 当前无对应存储列固定为空。
 */
@Data
@ApiModel("审计日志信息")
public class AuditLogVO {

    @ApiModelProperty("日志ID")
    private Long id;

    @ApiModelProperty("操作模块：user/department/.../system")
    private String module;

    @ApiModelProperty("操作类型：create/update/delete/query/...")
    private String action;

    @ApiModelProperty("操作描述")
    private String description;

    @ApiModelProperty("操作人")
    private String operator;

    @ApiModelProperty("操作IP")
    private String operatorIp;

    @ApiModelProperty("请求方式")
    private String requestMethod;

    @ApiModelProperty("请求URL")
    private String requestUrl;

    @ApiModelProperty("请求参数")
    private String requestParams;

    @ApiModelProperty("响应结果（当前留空）")
    private String responseResult;

    @ApiModelProperty("状态：1-成功 0-失败")
    private Integer status;

    @ApiModelProperty("耗时（毫秒）")
    private Integer duration;

    @ApiModelProperty("操作时间")
    private String operateTime;

    @ApiModelProperty("错误信息")
    private String errorMsg;
}
