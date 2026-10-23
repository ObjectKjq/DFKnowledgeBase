package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.controller.doc.vo.DocCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.controller.doc.vo.DocRespVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.service.doc.DocService;
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

import static com.df.knowledge.common.CommonResult.success;

/**
 * 文档 Controller
 *
 * 接口仅要求登录，内容权限由服务层按知识库成员关系判定（PRD §8.1 权限矩阵）
 */
@Tag(name = "docController")
@RestController
@RequestMapping("/knowledge/doc")
@Validated
public class DocController {

    @Resource
    private DocService docService;

    @PostMapping("/create")
    @Operation(summary = "创建文档", description = "两步式上传：先调文件上传接口拿文件编号与下载链接，再保存文档；格式白名单 PDF/Word/PPT/Excel/TXT/Markdown，创建成功进入「排队」")
    public CommonResult<Long> createDoc(@Valid @RequestBody DocCreateReqVO reqVO) {
        return success(docService.createDoc(StpUtil.getLoginIdAsLong(), reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询文档", description = "仅返回当前用户有权访问的知识库范围内的文档，可按知识库、标题、状态收窄")
    public CommonResult<PageResult<DocRespVO>> getDocPage(@Valid DocPageReqVO reqVO) {
        PageResult<DocDO> pageResult = docService.getDocPage(StpUtil.getLoginIdAsLong(), reqVO);
        return success(PageResult.build(pageResult.getTotal(),
                BeanUtil.copyToList(pageResult.getList(), DocRespVO.class)));
    }

    @GetMapping("/get")
    @Operation(summary = "获取文档详情", description = "失败文档附失败原因；非成员的请求按知识库不存在拒绝")
    @Parameter(name = "id", description = "文档编号", required = true, example = "1")
    public CommonResult<DocRespVO> getDoc(@RequestParam("id") Long id) {
        DocDO doc = docService.getDoc(StpUtil.getLoginIdAsLong(), id);
        return success(BeanUtil.copyProperties(doc, DocRespVO.class));
    }

    @PostMapping("/retry")
    @Operation(summary = "重试入库", description = "失败文档回到「排队」重新执行；重复失败保留首次原因")
    @Parameter(name = "id", description = "文档编号", required = true, example = "1")
    public CommonResult<Boolean> retryDoc(@RequestParam("id") Long id) {
        docService.retryDoc(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文档", description = "软删除，30 天内可恢复到删除前状态")
    @Parameter(name = "id", description = "文档编号", required = true, example = "1")
    public CommonResult<Boolean> deleteDoc(@RequestParam("id") Long id) {
        docService.deleteDoc(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @PostMapping("/restore")
    @Operation(summary = "恢复文档", description = "仅 30 天保留期内可恢复；原可用文档须重建索引后才可用，原处理中重新排队，原失败保留失败原因")
    @Parameter(name = "id", description = "文档编号", required = true, example = "1")
    public CommonResult<Boolean> restoreDoc(@RequestParam("id") Long id) {
        docService.restoreDoc(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }
    @DeleteMapping("/purge")
    @Operation(summary = "彻底删除回收站文档", description = "需要再次确认，无其他业务引用时回收文件字节")
    public CommonResult<Boolean> purgeDoc(@RequestParam("id") Long id,
            @RequestParam(value = "confirmed", defaultValue = "false") boolean confirmed) {
        docService.purgeDoc(StpUtil.getLoginIdAsLong(), id, confirmed);
        return success(true);
    }

}
