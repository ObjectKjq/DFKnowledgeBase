package com.df.knowledge.controller.rag;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.rag.vo.RagAnswerRespVO;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.controller.rag.vo.RagCitationRespVO;
import com.df.knowledge.service.rag.RagAnswerResult;
import com.df.knowledge.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.df.knowledge.common.CommonResult.success;

/**
 * RAG 检索 Controller
 */
@Tag(name = "ragController")
@RestController
@RequestMapping("/knowledge/rag")
@Validated
public class RagController {

    @Resource
    private RagService ragService;

    @PostMapping("/ask")
    @Operation(summary = "RAG 检索", description = "在当前用户会话中同步执行权限过滤、混合检索和带引用答案生成")
    public CommonResult<RagAnswerRespVO> ask(@Valid @RequestBody RagAskReqVO reqVO) {
        RagAnswerResult result = ragService.ask(StpUtil.getLoginIdAsLong(), reqVO);
        RagAnswerRespVO response = BeanUtil.copyProperties(result, RagAnswerRespVO.class);
        response.setCitations(BeanUtil.copyToList(result.getCitations(), RagCitationRespVO.class));
        return success(response);
    }
}
