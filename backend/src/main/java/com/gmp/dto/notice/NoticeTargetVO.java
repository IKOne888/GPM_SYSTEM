package com.gmp.dto.notice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 通知公告可选接收对象 VO
 * <p>
 * 供前端「接收范围」选择使用：全员无需选择；按部门/按岗位时分别使用 departments / positions。
 */
@Data
@ApiModel("公告接收对象选项")
public class NoticeTargetVO {

    /** 启用状态的部门列表 */
    @ApiModelProperty("部门列表")
    private List<Item> departments;

    /** 启用状态的岗位列表 */
    @ApiModelProperty("岗位列表")
    private List<Item> positions;

    /**
     * 选项条目（id + 名称）
     */
    @Data
    @ApiModel("选项条目")
    public static class Item {

        /** 选项ID */
        @ApiModelProperty("选项ID")
        private Long id;

        /** 选项名称 */
        @ApiModelProperty("选项名称")
        private String name;

        /**
         * 构造选项
         *
         * @param id   选项ID
         * @param name 选项名称
         */
        public Item(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}