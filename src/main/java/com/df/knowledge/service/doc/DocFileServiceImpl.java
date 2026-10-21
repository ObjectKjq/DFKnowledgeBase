package com.df.knowledge.service.doc;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档存储 Service 实现类（本地文件系统）
 */
@Service
@Validated
@Slf4j
public class DocFileServiceImpl implements DocFileService {

    /**
     * 文档落盘根目录（配置项 knowledge.doc.storage-path）
     */
    @Value("${knowledge.doc.storage-path}")
    private String storagePath;

    @Override
    public String store(Long knowledgeBaseId, String extension, MultipartFile file) {
        // 1. 存储目录按知识库隔离，文件名用随机 UUID：同名文件允许重复上传且互不覆盖（PRD FR-3）
        File dir = FileUtil.file(storagePath, String.valueOf(knowledgeBaseId));
        FileUtil.mkdir(dir);
        File target = FileUtil.file(dir, IdUtil.fastSimpleUUID() + "." + extension);
        // 2. 落盘失败给出明确业务提示
        try {
            file.transferTo(target);
        } catch (IOException | IllegalStateException ex) {
            log.error("[store][文件({})存储失败]", file.getOriginalFilename(), ex);
            throw new ServiceException(ErrorCodeConstants.DOC_FILE_STORE_FAILED);
        }
        return target.getPath();
    }

    @Override
    public InputStream open(String filePath) throws IOException {
        return Files.newInputStream(Path.of(filePath));
    }

    @Override
    public void delete(String filePath) {
        FileUtil.del(filePath);
    }
}
