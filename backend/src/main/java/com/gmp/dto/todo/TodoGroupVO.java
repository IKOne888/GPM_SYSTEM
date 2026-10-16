package com.gmp.dto.todo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 待办分组 VO（某类待办的总数 + Top 明细）
 */
@Data
@ApiModel("待办分组")
public class TodoGroupVO {

    /** 该类型待办总数 */
    @ApiModelProperty("待办总数")
    private Long count;

    /** Top N 明细（最多 5 条） */
    @ApiModelProperty("Top 明细")
    private List<TodoItemVO> items;
}