package com.df.knowledge.dal.mysql.doc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档分块 Mapper
 */
@Mapper
public interface DocChunkMapper extends BaseMapper<DocChunkDO> {

    /**
     * 按文档查询分块，按原文顺序返回
     *
     * @param docId 文档编号
     * @return 分块列表
     */
    default List<DocChunkDO> selectListByDocId(Long docId) {
        return selectList(new LambdaQueryWrapper<DocChunkDO>()
                .eq(DocChunkDO::getDocId, docId)
                .orderByAsc(DocChunkDO::getChunkIndex));
    }

    /**
     * 物理清理文档的旧分块，供重试替换及彻底删除使用
     *
     * @param docId 文档编号
     * @return 删除条数
     */
    @Delete("DELETE FROM knowledge_doc_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") Long docId);
}
