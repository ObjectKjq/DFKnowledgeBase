package com.df.knowledge.controller.knowledge;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseRespVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBaseSaveReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.service.knowledge.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 知识库 Controller
 *
 * 接口仅要求登录，内容权限由服务层按知识库成员关系判定（PRD §8.1 权限矩阵）
 */
@Tag(name = "knowledgeBaseController")
@RestController
@RequestMapping("/knowledge/base")
@Validated
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库", description = "创建时指定类型（个人/共享），所有者为当前登录用户")
    public CommonResult<Long> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseSaveReqVO reqVO) {
        return success(knowledgeBaseService.createKnowledgeBase(StpUtil.getLoginIdAsLong(), reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库", description = "默认知识库不可删除；非成员的请求按知识库不存在拒绝")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1")
    public CommonResult<Boolean> deleteKnowledgeBase(@RequestParam("id") Long id) {
        knowledgeBaseService.deleteKnowledgeBase(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @GetMapping("/recycle-list")
    @Operation(summary = "查看本人知识库回收站")
    public CommonResult<List<KnowledgeBaseRespVO>> getDeletedKnowledgeBases() {
        return success(BeanUtil.copyToList(knowledgeBaseService.getDeletedKnowledgeBases(StpUtil.getLoginIdAsLong()), KnowledgeBaseRespVO.class));
    }

    @PostMapping("/restore")
    @Operation(summary = "恢复知识库", description = "仅原所有者可在 30 天保留期内恢复，保留原成员关系")
    public CommonResult<Boolean> restoreKnowledgeBase(@RequestParam("id") Long id) {
        knowledgeBaseService.restoreKnowledgeBase(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @DeleteMapping("/purge")
    @Operation(summary = "彻底删除回收站知识库", description = "需要再次确认，不可恢复")
    public CommonResult<Boolean> purgeKnowledgeBase(@RequestParam("id") Long id,
            @RequestParam(value = "confirmed", defaultValue = "false") boolean confirmed) {
        knowledgeBaseService.purgeKnowledgeBase(StpUtil.getLoginIdAsLong(), id, confirmed);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取知识库详情")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1")
    public CommonResult<KnowledgeBaseRespVO> getKnowledgeBase(@RequestParam("id") Long id) {
        KnowledgeBaseDO knowledgeBase = knowledgeBaseService.getKnowledgeBase(StpUtil.getLoginIdAsLong(), id);
        return success(BeanUtil.copyProperties(knowledgeBase, KnowledgeBaseRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询知识库", description = "仅返回当前用户有权访问的知识库（自己拥有的 + 自己是成员的共享库）")
    public CommonResult<PageResult<KnowledgeBaseRespVO>> getKnowledgeBasePage(@Valid KnowledgeBasePageReqVO reqVO) {
        PageResult<KnowledgeBaseDO> pageResult = knowledgeBaseService.getKnowledgeBasePage(StpUtil.getLoginIdAsLong(), reqVO);
        return success(PageResult.build(pageResult.getTotal(),
                BeanUtil.copyToList(pageResult.getList(), KnowledgeBaseRespVO.class)));
    }
}
