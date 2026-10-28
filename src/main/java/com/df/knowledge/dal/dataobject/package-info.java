/**
 * 数据对象层：MySQL 的 DO、公共基类 BaseDO 与 ES 的 Document
 *
 * DO 统一 @TableName + @Data，继承 BaseDO 获得 id / 审计字段 / 逻辑删除
 * ES 实体使用 @Document + @Field，独立于 DO，不继承 MySQL 的公共字段
 */
package com.df.knowledge.dal.dataobject;
