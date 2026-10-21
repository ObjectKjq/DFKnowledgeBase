package com.df.knowledge.service.doc;

import java.util.List;

/**
 * 文档向量化端口，生产实现使用内置 ONNX 模型
 */
public interface DocEmbeddingService {

    /**
     * 批量向量化，结果顺序与输入一致
     *
     * @param texts 分块文本列表
     * @return 每条文本对应的 512 维向量
     */
    List<float[]> embedAll(List<String> texts);
}
