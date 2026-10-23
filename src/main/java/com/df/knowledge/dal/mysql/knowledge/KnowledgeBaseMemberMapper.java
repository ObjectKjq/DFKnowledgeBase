package com.df.knowledge.dal.mysql.knowledge;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识库成员 Mapper
 */
@Mapper
public interface KnowledgeBaseMemberMapper extends BaseMapper<KnowledgeBaseMemberDO> {
    /** 彻底删除知识库成员关系。
     * @param knowledgeBaseId 知识库编号
     * @return 清理条数
     */
    @Delete("DELETE FROM knowledge_base_member WHERE knowledge_base_id = #{knowledgeBaseId}")
    int purgeByKnowledgeBaseId(Long knowledgeBaseId);


    /**
     * 查询知识库的成员关联
     *
     * @param knowledgeBaseId 知识库编号
     * @param userId 成员用户编号
     * @return 成员关联（不存在时返回 null）
     */
    default KnowledgeBaseMemberDO selectByKnowledgeBaseIdAndUserId(Long knowledgeBaseId, Long userId) {
        // 「知识库 + 用户」唯一性由服务层保证，异常重复数据取最早一条，避免多行时报错
        List<KnowledgeBaseMemberDO> members = selectList(new LambdaQueryWrapper<KnowledgeBaseMemberDO>()
                .eq(KnowledgeBaseMemberDO::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeBaseMemberDO::getUserId, userId)
                .orderByAsc(KnowledgeBaseMemberDO::getId));
        return CollUtil.isEmpty(members) ? null : members.get(0);
    }

    /**
     * 查询知识库的成员关联列表
     *
     * @param knowledgeBaseId 知识库编号
     * @return 成员关联列表
     */
    default List<KnowledgeBaseMemberDO> selectListByKnowledgeBaseId(Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBaseMemberDO>()
                .eq(KnowledgeBaseMemberDO::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(KnowledgeBaseMemberDO::getId));
    }

    /**
     * 查询用户加入的成员关联列表
     *
     * @param userId 成员用户编号
     * @return 成员关联列表
     */
    default List<KnowledgeBaseMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBaseMemberDO>().eq(KnowledgeBaseMemberDO::getUserId, userId));
    }

    /**
     * 删除知识库的全部成员关联
     *
     * @param knowledgeBaseId 知识库编号
     */
    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<KnowledgeBaseMemberDO>()
                .eq(KnowledgeBaseMemberDO::getKnowledgeBaseId, knowledgeBaseId));
    }
}
