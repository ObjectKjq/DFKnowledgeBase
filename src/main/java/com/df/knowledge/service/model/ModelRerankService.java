package com.df.knowledge.service.model;

import com.df.knowledge.service.doc.DocChunkResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 可选真实重排；未配置时保持融合顺序，配置后故障显式向调用方报告。 */
@Service
@RequiredArgsConstructor
public class ModelRerankService {

    private final ModelConfigurationService configurationService;
    private final OpenAiCompatibleModelClient client;

    /**
     * 对授权候选执行可选重排。
     * @param query 检索问题
     * @param results 权限过滤后的融合候选
     * @return 保留原对象及元数据的重排结果
     */
    public List<DocChunkResult> rerank(String query, List<DocChunkResult> results) {
        if (results.isEmpty()) {
            return results;
        }
        var model = configurationService.list().stream().filter(item -> "RERANK".equals(item.getKind())).findFirst();
        if (model.isEmpty()) {
            return results;
        }
        return client.rerank(model.get(), query, results.stream().map(DocChunkResult::getText).toList())
                .stream().map(results::get).toList();
    }
}
