package com.df.knowledge.dal.mysql.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseDO> {

    /**
     * 查询用户拥有的知识库列表
     *
     * @param ownerId 所有者用户编号
     * @return 知识库列表
     */
    default List<KnowledgeBaseDO> selectListByOwnerId(Long ownerId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBaseDO>().eq(KnowledgeBaseDO::getOwnerId, ownerId));
    }

    /**
     * 分页查询指定范围内的知识库
     *
     * @param reqVO 分页查询条件
     * @param ids 知识库编号范围（有权访问的知识库）
     * @return 知识库分页
     */
    default Page<KnowledgeBaseDO> selectPage(KnowledgeBasePageReqVO reqVO, Collection<Long> ids) {
        // 作用域过滤不允许跳过：空范围直接返回空页，避免放行全表
        if (CollUtil.isEmpty(ids)) {
            return new Page<>(reqVO.getPageNum(), reqVO.getPageSize());
        }
        return selectPage(new Page<>(reqVO.getPageNum(), reqVO.getPageSize()),
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .in(KnowledgeBaseDO::getId, ids)
                        .like(StrUtil.isNotBlank(reqVO.getName()), KnowledgeBaseDO::getName, reqVO.getName())
                        .eq(reqVO.getType() != null, KnowledgeBaseDO::getType, reqVO.getType())
                        .orderByDesc(KnowledgeBaseDO::getId));
    }
}
