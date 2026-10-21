package com.df.knowledge.enums.doc;

/**
 * 文档常量
 */
public interface DocConstants {

    // ========== 文件上传 ==========
    long MAX_FILE_SIZE = 50L * 1024 * 1024; // 单文件大小上限（字节，50 MB，PRD FR-3）

    // ========== 文档切分 ==========
    int CHUNK_SIZE = 500; // 分块 Unicode 字符数上限
    int CHUNK_OVERLAP = 100; // 同页相邻分块重叠 Unicode 字符数

    // ========== 软删除 ==========
    int SOFT_DELETE_RETENTION_DAYS = 30; // 软删除保留天数（期内可恢复，超期自动彻底删除，PRD §8.2）
}
