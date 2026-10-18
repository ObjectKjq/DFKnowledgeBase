package com.df.knowledge.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数
 *
 * 各业务的 PageReqVO 继承本类，获得分页参数
 */
@Data
public class PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认页码
     */
    private static final Integer PAGE_NUM_DEFAULT = 1;

    /**
     * 默认每页条数
     */
    private static final Integer PAGE_SIZE_DEFAULT = 10;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer pageNum = PAGE_NUM_DEFAULT;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = PAGE_SIZE_DEFAULT;
}
