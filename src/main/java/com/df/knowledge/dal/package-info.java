/**
 * 数据访问层：DO 与 MyBatis-Plus Mapper
 *
 * 1. dataobject 放 DO，mysql 放 Mapper，两者按业务再分包
 * 2. 表名统一 system_{业务} 前缀，公共字段见 {@link com.df.knowledge.dal.dataobject.BaseDO}
 */
package com.df.knowledge.dal;
