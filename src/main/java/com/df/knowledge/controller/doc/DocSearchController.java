package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.doc.vo.DocChunkRespVO;
import com.df.knowledge.controller.doc.vo.DocDocumentPreviewRespVO;
import com.df.knowledge.controller.doc.vo.DocGuideReqVO;
import com.df.knowledge.controller.doc.vo.DocGuideRespVO;
import com.df.knowledge.controller.doc.vo.DocSearchPageRespVO;
import com.df.knowledge.controller.doc.vo.DocSearchReqVO;
import com.df.knowledge.service.doc.DocGuideService;
import com.df.knowledge.service.doc.DocSearchPage;
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

    @Resource
    private DocGuideService docGuideService;

    @PostMapping("/guide")
    @Operation(summary = "引导检索", description = "最多两轮澄清；可跳过，模型失败后说明原因并执行混合检索")
    public CommonResult<DocGuideRespVO> guide(
            @Valid @RequestBody DocGuideReqVO request) {
        var result = docGuideService.guide(StpUtil.getLoginIdAsLong(), request);
        var response = BeanUtil.copyProperties(result, DocGuideRespVO.class);
        if (result.getSearch() != null) {
            response.setSearch(toPageResponse(result.getSearch()));
        }
        return success(response);
    }

    @PostMapping("/search")
    @Operation(summary = "混合或模糊检索", description = "按当前权限和文档过滤返回分页，默认每页 20 条；达到候选上限时显式提示缩小范围")
    public CommonResult<DocSearchPageRespVO> search(@Valid @RequestBody DocSearchReqVO reqVO) {
        var result = docSearchService.search(StpUtil.getLoginIdAsLong(), reqVO);
        return success(toPageResponse(result));
    }

    private DocSearchPageRespVO toPageResponse(DocSearchPage result) {
        var response = new DocSearchPageRespVO();
        response.setTotal(result.getTotal());
        response.setList(BeanUtil.copyToList(result.getList(), DocChunkRespVO.class));
        response.setIndexingCount(result.getIndexingCount());
        response.setCandidateLimitReached(result.isCandidateLimitReached());
        return response;
    }

    @GetMapping("/document-preview")
    @Operation(summary = "预览整份解析文档", description = "重新授权并读取全部解析分块及页码/工作表位置，提供原始文件兜底")
    @Parameter(name = "chunkId",description = "原命中分块编号",required = true)
    public CommonResult<DocDocumentPreviewRespVO> getDocumentPreview(@RequestParam("chunkId") Long chunkId) {
        var result = docSearchService.getDocumentPreview(StpUtil.getLoginIdAsLong(), chunkId);
        var response = new DocDocumentPreviewRespVO();
        response.setSource(BeanUtil.copyProperties(result.getSource(), DocChunkRespVO.class));
        response.setChunks(BeanUtil.copyToList(result.getChunks(), DocChunkRespVO.class));
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
