package com.gmp.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果VO
 * <p>
 * 返回成功条数、失败条数以及失败原因列表，便于用户定位问题数据。
 */
@Data
@ApiModel("导入结果")
public class ImportResultVO {

    @ApiModelProperty("成功条数")
    private Integer successCount = 0;

    @ApiModelProperty("失败条数")
    private Integer failCount = 0;

    @ApiModelProperty("失败原因列表")
    private List<String> failMessages = new ArrayList<>();

    /**
     * 记录一条成功
     */
    public void addSuccess() {
        this.successCount++;
    }

    /**
     * 记录一条失败并追加原因
     *
     * @param message 失败原因
     */
    public void addFail(String message) {
        this.failCount++;
        this.failMessages.add(message);
    }
}
