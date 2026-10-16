package com.df.knowledge.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页返回结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /**
     * 总量
     */
    private Long total;

    /**
     * 数据列表
     */
    private List<T> list;

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, Collections.emptyList());
    }

    public static <T> PageResult<T> build(Long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
