package com.df.knowledge.enums.doc;

/**
 * 文档常量
 */
public interface DocConstants {

    // ========== 文件上传 ==========
    long MAX_FILE_SIZE = 50L * 1024 * 1024; // 单文件大小上限（字节，50 MB，PRD FR-3）

    // ========== 软删除 ==========
    int SOFT_DELETE_RETENTION_DAYS = 30; // 软删除保留天数（期内可恢复，超期自动彻底删除，PRD §8.2）
}
