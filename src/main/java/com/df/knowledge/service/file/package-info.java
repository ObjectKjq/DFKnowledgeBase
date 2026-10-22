/**
 * 文件业务层：通用文件的上传、读取与删除
 *
 * 1. 文件本体存数据库（system_file + system_file_content，ADR-0009），业务表只落文件引用
 * 2. 删除为逻辑删除且不做引用/归属校验，标记后立即 404
 */
package com.df.knowledge.service.file;
