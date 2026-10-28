/**
 * 数据访问层：MySQL 数据对象、ES 索引实体与对应访问接口
 *
 * 1. dataobject 放 DO 与 Document，mysql 放 Mapper，elasticsearch 放 Repository，均按业务再分包
 * 2. 表名统一 system_{业务} 前缀，公共字段见 {@link com.df.knowledge.dal.dataobject.BaseDO}
 */
package com.df.knowledge.dal;
