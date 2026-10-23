package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.doc.vo.DocChunkRespVO;
import com.df.knowledge.controller.doc.vo.DocSearchReqVO;
import com.df.knowledge.service.doc.DocSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 混合检索与原文预览 Controller
 */
@Tag(name = "docSearchController")
@RestController
@RequestMapping("/knowledge/search")
@Validated
public class DocSearchController {

    @Resource
    private DocSearchService docSearchService;

    @PostMapping("/search")
    @Operation(summary = "混合检索", description = "仅检索当前用户有权范围内的可用文档，返回最多 20 个融合排序分块")
    public CommonResult<com.df.knowledge.controller.doc.vo.DocSearchPageRespVO> search(@Valid @RequestBody DocSearchReqVO reqVO) {
        var result = docSearchService.search(StpUtil.getLoginIdAsLong(), reqVO);
        var response = new com.df.knowledge.controller.doc.vo.DocSearchPageRespVO();
        response.setTotal(result.getTotal());
        response.setList(BeanUtil.copyToList(result.getList(), DocChunkRespVO.class));
        response.setIndexingCount(result.getIndexingCount());
        response.setCandidateLimitReached(result.isCandidateLimitReached());
        return success(response);
    }

    @GetMapping("/preview")
    @Operation(summary = "预览分块原文", description = "重新校验内容权限和文档可用状态，返回原文及来源定位")
    @Parameter(name = "chunkId", description = "分块编号", required = true, example = "1")
    public CommonResult<DocChunkRespVO> getPreview(@RequestParam("chunkId") Long chunkId) {
        return success(BeanUtil.copyProperties(docSearchService.getPreview(StpUtil.getLoginIdAsLong(), chunkId),
                DocChunkRespVO.class));
    }
}
