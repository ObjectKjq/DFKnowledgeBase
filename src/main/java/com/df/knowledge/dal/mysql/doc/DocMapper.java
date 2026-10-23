package com.df.knowledge.dal.mysql.doc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.enums.doc.DocStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocMapper extends BaseMapper<DocDO> {

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
     * 查询指定状态集合的文档（启动恢复入库任务用）
     *
     * @param statuses 状态集合，枚举 {@link DocStatusEnum}
     * @return 文档列表
     */
    default List<DocDO> selectListByStatuses(Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapper<DocDO>().in(DocDO::getStatus, statuses)
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
     * 查询删除时间早于指定时间的软删除文档（超期彻底删除用）
     *
     * @param status 文档状态（已删除），枚举 {@link DocStatusEnum}
     * @param maxDeleteTime 删除时间上限
     * @return 文档列表
     */
    default List<DocDO> selectListByStatusAndDeleteTimeBefore(Integer status, LocalDateTime maxDeleteTime) {
        return selectList(new LambdaQueryWrapper<DocDO>()
                .eq(DocDO::getStatus, status)
                .lt(DocDO::getDeleteTime, maxDeleteTime));
    }

    /**
     * 查询仍指向磁盘存量文件的文档（file_id 为空且 file_path 非空，存量迁移用）
     *
     * @return 文档列表
     */
    default List<DocDO> selectListByLegacyFile() {
        return selectList(new LambdaQueryWrapper<DocDO>()
                .isNull(DocDO::getFileId)
                .isNotNull(DocDO::getFilePath)
                .ne(DocDO::getFilePath, ""));
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

    /**
     * 持久保存本次提交的排序时间；状态仍是权威任务状态。
     * @param id 文档编号
     * @return 更新条数
     */
    default int updateIngestSubmitTime(Long id) {
        return update(null, new LambdaUpdateWrapper<DocDO>().eq(DocDO::getId, id)
                .in(DocDO::getStatus, DocStatusEnum.IN_FLIGHT_STATUS_CODES)
                .set(DocDO::getIngestSubmitTime, LocalDateTime.now()));
    }

    /**
     * 改写文件引用（存量磁盘文件迁移入库后落 file_id / file_url 并清空过渡字段 file_path）
     *
     * @param id 文档编号
     * @param fileId 文件编号
     * @param fileUrl 文件下载链接
     * @return 更新条数
     */
    default int updateFileRef(Long id, Long fileId, String fileUrl) {
        return update(null, new LambdaUpdateWrapper<DocDO>()
                .eq(DocDO::getId, id)
                .set(DocDO::getFileId, fileId)
                .set(DocDO::getFileUrl, fileUrl)
                .set(DocDO::getFilePath, null));
    }
}
