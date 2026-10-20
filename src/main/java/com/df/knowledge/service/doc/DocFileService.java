package com.df.knowledge.service.doc;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档存储 Service 接口（外部端口：文件系统）
 *
 * 上传落盘与彻底删除时的文件清理都经由本端口，测试在服务层将其桩掉
 */
public interface DocFileService {

    /**
     * 保存上传文件到存储目录
     *
     * @param knowledgeBaseId 知识库编号（存储目录按知识库隔离）
     * @param extension 文件扩展名（小写）
     * @param file 上传文件
     * @return 文件存储路径
     */
    String store(Long knowledgeBaseId, String extension, MultipartFile file);

    /**
     * 删除存储的文件（文档彻底删除时清理，文件不存在时静默跳过）
     *
     * @param filePath 文件存储路径
     */
    void delete(String filePath);
}
