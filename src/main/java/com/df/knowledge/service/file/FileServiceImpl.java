package com.df.knowledge.service.file;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.file.FileContentDO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.file.FileContentMapper;
import com.df.knowledge.dal.mysql.file.FileMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.file.FileConstants;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件 Service 实现类（文件本体存数据库，ADR-0009）
 */
@Service
@Validated
@Slf4j
public class FileServiceImpl implements FileService {

    /**
     * 应用 context-path（下载链接前缀，配置项 server.servlet.context-path）
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private FileContentMapper fileContentMapper;

    @Resource
    private DocMapper docMapper;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileDO upload(MultipartFile file) {
        try {
            return createFile(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException ex) {
            log.error("[upload][文件({})读取失败]", file.getOriginalFilename(), ex);
            throw new ServiceException(ErrorCodeConstants.FILE_STORE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileDO createFile(byte[] content, String name, String type) {
        // 1. 文件非空
        if (ArrayUtil.isEmpty(content)) {
            throw new ServiceException(ErrorCodeConstants.FILE_IS_EMPTY);
        }
        // 2. 扩展名 + Content-Type 双校验（格式白名单，防伪装）
        validateFileType(name, type);
        // 3. 单文件大小上限 50 MB
        if (content.length > FileConstants.MAX_FILE_SIZE) {
            throw new ServiceException(ErrorCodeConstants.FILE_TOO_LARGE);
        }
        // 4. 元数据与字节分表落库
        FileDO file = new FileDO().setName(name).setType(type).setSize((long) content.length);
        if (RequestContextHolder.getRequestAttributes() != null
                && StpUtil.isLogin()) {
            file.setUploaderId(StpUtil.getLoginIdAsLong());
        }
        fileMapper.insert(file);
        fileContentMapper.insert(new FileContentDO().setFileId(file.getId()).setContent(content));
        return file;
    }

    @Override
    public FileDO getFile(Long id) {
        return fileMapper.selectById(id);
    }

    @Override
    public byte[] getContent(Long id) {
        FileContentDO fileContent = fileContentMapper.selectByFileId(id);
        return fileContent != null ? fileContent.getContent() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 只有最后一个文档引用移除后才回收文件字节。
        fileMapper.selectByIdForUpdate(id);
        if (!docMapper.selectListByFileIdForUpdate(id).isEmpty()) {
            return;
        }
        fileContentMapper.purgeByFileId(id);
        fileMapper.purgeById(id);
    }

    @Override
    public FileDO getReadableFile(Long userId, Long id) {
        FileDO file = fileMapper.selectById(id);
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        var references = docMapper.selectListByFileId(id);
        if (references.isEmpty() && userId.equals(file.getUploaderId())) {
            return file;
        }
        for (var doc : references) {
            try {
                knowledgeBasePermissionService.checkViewPermission(userId, doc.getKnowledgeBaseId());
                return file;
            } catch (ServiceException ignored) {
                // 共享文件可通过任意当前可见引用读取；上传者不享有越权例外。
            }
        }
        throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
    }

    @Override
    public FileDO getAttachableFile(Long userId, Long id) {
        FileDO file = fileMapper.selectByIdForUpdate(id);
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        var references = docMapper.selectListByFileIdForUpdate(id);
        if (references.isEmpty() && userId.equals(file.getUploaderId())) {
            return file;
        }
        for (var doc : references) {
            try {
                knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
                return file;
            } catch (ServiceException ignored) {
                // 关联属于写入操作，查看者不能复制内容到个人知识库。
            }
        }
        throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        FileDO file = fileMapper.selectByIdForUpdate(id);
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        var references = docMapper.selectListByFileIdForUpdate(id);
        if (!references.isEmpty()) {
            for (var doc : references) {
                try {
                    knowledgeBasePermissionService.checkContentWritePermission(userId, doc.getKnowledgeBaseId());
                } catch (ServiceException ignored) {
                    throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
                }
            }
            throw new ServiceException(ErrorCodeConstants.FILE_IN_USE);
        }
        if (!userId.equals(file.getUploaderId())) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        delete(id);
    }

    @Override
    public String buildDownloadUrl(Long id) {
        return contextPath + "/system/file/download?id=" + id;
    }

    /**
     * 校验文件类型：扩展名与 Content-Type 必须同时落在白名单内
     *
     * @param name 原始文件名
     * @param type 文件 MIME 类型（Content-Type）
     */
    private void validateFileType(String name, String type) {
        String extension = FileUtil.extName(StrUtil.nullToEmpty(name)).toLowerCase();
        List<String> allowedTypes = FileConstants.ALLOWED_CONTENT_TYPES.get(extension);
        if (allowedTypes == null || type == null || !allowedTypes.contains(type.toLowerCase())) {
            throw new ServiceException(ErrorCodeConstants.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
