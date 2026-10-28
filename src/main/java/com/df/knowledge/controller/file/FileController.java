package com.df.knowledge.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.file.vo.FileRespVO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 文件 Controller
 *
 * 文件上传、下载与删除，按当前业务内容权限校验（ADR-0009）
 */
@Tag(name = "fileController")
@RestController
@RequestMapping("/system/file")
@Validated
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "格式白名单：PDF/Word/PPT/Excel/TXT/Markdown 与常见图片，单文件上限 50 MB；返回文件编号与下载链接，业务侧保存链接引用")
    @Parameter(name = "file", description = "上传文件", required = true)
    public CommonResult<FileRespVO> uploadFile(@RequestParam("file") MultipartFile file) {
        FileDO result = fileService.upload(file);
        return success(convert(result));
    }

    @GetMapping("/download")
    @Operation(summary = "下载文件", description = "按业务内容权限返回文件字节流：图片与 PDF 内联预览，其余附件下载；无权与不存在统一返回文件不存在")
    @Parameter(name = "id", description = "文件编号", required = true, example = "1")
    public void downloadFile(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        FileDO file = fileService.getReadableFile(StpUtil.getLoginIdAsLong(), id);
        byte[] content = file != null ? fileService.getContent(id) : null;
        if (content == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        // 1. 按上传时声明的 MIME 输出，并禁止浏览器嗅探改判，防内联内容被当脚本执行
        response.setContentType(file.getType());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(content.length);
        // 2. 图片与 PDF 内联预览，其余附件下载；文件名同时给 ASCII 回退与 RFC 5987 编码（中文名）
        String disposition = isInline(file.getType()) ? "inline" : "attachment";
        String fallbackName = file.getName().replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                disposition + "; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encodedName);
        response.getOutputStream().write(content);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "仅上传者可删除未关联文件；已关联文件须通过删除文档回收")
    @Parameter(name = "id", description = "文件编号", required = true, example = "1")
    public CommonResult<Boolean> deleteFile(@RequestParam("id") Long id) {
        fileService.delete(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    /**
     * 转换文件 VO（补下载链接）
     */
    private FileRespVO convert(FileDO file) {
        FileRespVO respVO = BeanUtil.copyProperties(file, FileRespVO.class);
        respVO.setUrl(fileService.buildDownloadUrl(file.getId()));
        return respVO;
    }

    /**
     * 图片与 PDF 支持浏览器内联预览
     */
    private boolean isInline(String type) {
        return type.startsWith("image/") || "application/pdf".equals(type);
    }
}
