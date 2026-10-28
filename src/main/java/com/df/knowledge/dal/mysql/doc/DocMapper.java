package com.df.knowledge.dal.mysql.doc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocMapper extends BaseMapper<DocDO> {

    /**
     * @param filePath 历史磁盘文件路径
     * @return 仍保留该文件引用的文档数量
     */
    default Long countByFilePath(String filePath) {
        return selectCount(new LambdaQueryWrapper<DocDO>().eq(DocDO::getFilePath, filePath));
    }

    /** 查询全部仍保留的文档引用。
     * @param fileId 文件编号
     * @return 文档列表
     */
    default List<DocDO> selectListByFileId(Long fileId) {
        return selectList(new LambdaQueryWrapper<DocDO>().eq(DocDO::getFileId, fileId));
    }

    /** 读取最新引用并加锁，避免 REPEATABLE READ 旧快照漏掉刚提交的关联。
     * @param fileId 文件编号
     * @return 当前仍保留的文档引用
     */
    default List<DocDO> selectListByFileIdForUpdate(Long fileId) {
        return selectList(new LambdaQueryWrapper<DocDO>().eq(DocDO::getFileId, fileId).last("FOR UPDATE"));
    }

    /** 查询知识库的全部文档。
     * @param knowledgeBaseId 知识库编号
     * @return 文档列表
     */
    default List<DocDO> selectListByKnowledgeBaseId(Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<DocDO>().eq(DocDO::getKnowledgeBaseId, knowledgeBaseId));
    }

    /** 物理清理文档。
     * @param id 文档编号
     * @return 清理条数
     */
    @Delete("DELETE FROM knowledge_doc WHERE id = #{id}")
    int purgeById(Long id);

    /**
     * 锁定文档，串行化同一文档的分块替换与彻底删除
     *
     * @param id 文档编号
     * @return 文档，不存在时为空
     */
    default DocDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<DocDO>().eq(DocDO::getId, id).last("FOR UPDATE"));
    }

    /**
     * 分页查询指定范围内的文档
     *
     * @param reqVO 分页查询条件
     * @param knowledgeBaseIds 知识库编号范围（有权访问的知识库）
     * @return 文档分页
     */
    default Page<DocDO> selectPage(DocPageReqVO reqVO, Collection<Long> knowledgeBaseIds) {
        // 作用域过滤不允许跳过：空范围直接返回空页，避免放行全表
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return new Page<>(reqVO.getPageNum(), reqVO.getPageSize());
        }
        return selectPage(new Page<>(reqVO.getPageNum(), reqVO.getPageSize()),
                new LambdaQueryWrapper<DocDO>()
                        .in(DocDO::getKnowledgeBaseId, knowledgeBaseIds)
                        .eq(reqVO.getKnowledgeBaseId() != null, DocDO::getKnowledgeBaseId, reqVO.getKnowledgeBaseId())
                        .like(StrUtil.isNotBlank(reqVO.getTitle()), DocDO::getTitle, reqVO.getTitle())
                        .eq(reqVO.getStatus() != null, DocDO::getStatus, reqVO.getStatus())
                        .orderByDesc(DocDO::getId));
    }

    /**
     * 查询指定状态集合的文档，供定时任务扫描待处理及中断的入库任务
     *
     * @param statuses 状态集合，枚举 {@link DocStatusEnum}
     * @return 文档列表
     */
    default List<DocDO> selectListByStatuses(Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapper<DocDO>().in(DocDO::getStatus, statuses)
                .apply("EXISTS (SELECT 1 FROM knowledge_base kb WHERE kb.id = knowledge_doc.knowledge_base_id"
                        + " AND kb.deleted = 0)")
                .orderByAsc(DocDO::getIngestSubmitTime, DocDO::getId));
    }

    /**
     * 查询有权范围内处于指定状态的文档，供检索限定可用文档
     *
     * @param knowledgeBaseIds 知识库编号范围，不得为空
     * @param status 文档状态，枚举 {@link DocStatusEnum}
     * @return 文档列表
     */
    default List<DocDO> selectListByKnowledgeBaseIdsAndStatus(Collection<Long> knowledgeBaseIds, Integer status) {
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<DocDO>()
                .in(DocDO::getKnowledgeBaseId, knowledgeBaseIds)
                .eq(DocDO::getStatus, status));
    }

    /**
     * 状态迁移（CAS）：仅当当前状态为 fromStatus 时迁移到 toStatus，避免与用户操作并发时把已删除文档拉回流程
     *
     * @param id 文档编号
     * @param fromStatus 迁移前状态，枚举 {@link DocStatusEnum}
     * @param toStatus 迁移后状态，枚举 {@link DocStatusEnum}
     * @param failReason 失败原因（传空表示不更新，保留首次失败原因）
     * @return 迁移成功条数（0 表示状态已变化，未迁移）
     */
    default int updateStatus(Long id, Integer fromStatus, Integer toStatus, String failReason) {
        return update(null, new LambdaUpdateWrapper<DocDO>()
                .eq(DocDO::getId, id)
                .eq(DocDO::getStatus, fromStatus)
                .set(DocDO::getStatus, toStatus)
                .set(StrUtil.isNotBlank(failReason), DocDO::getFailReason, failReason));
    }

}
