package com.df.knowledge.service.file;

import com.df.knowledge.dal.dataobject.file.FileDO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件 Service 接口（外部端口：文件存储，本体存数据库）
 *
 * 通用文件上传、读取与删除（ADR-0009）：业务侧先上传拿文件引用，再保存业务数据
 */
public interface FileService {

    /**
     * 上传文件（格式白名单与 50 MB 上限校验后落库）
     *
     * @param file 上传文件
     * @return 文件元数据（含下载链接）
     */
    FileDO upload(MultipartFile file);

    /**
     * 按字节内容新增文件（存量磁盘文件迁移入库用）
     *
     * @param content 文件内容
     * @param name 原始文件名
     * @param type 文件 MIME 类型（Content-Type）
     * @return 文件元数据（含下载链接）
     */
    FileDO createFile(byte[] content, String name, String type);

    /**
     * 获取文件元数据
     *
     * @param id 文件编号
     * @return 文件元数据，不存在时为 null
     */
    FileDO getFile(Long id);

    /**
     * 读取文件内容
     *
     * @param id 文件编号
     * @return 文件内容，不存在时为 null
     */
    byte[] getContent(Long id);

    /**
     * 删除文件（逻辑删除，不做引用与归属校验，删除后立即不可下载）
     *
     * @param id 文件编号
     */
    void delete(Long id);

    /**
     * 构建文件下载链接（含 context-path 的相对路径）
     *
     * @param id 文件编号
     * @return 下载链接
     */
    String buildDownloadUrl(Long id);
}
