package com.df.knowledge.dal.mysql.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.file.FileContentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件内容 Mapper
 */
@Mapper
public interface FileContentMapper extends BaseMapper<FileContentDO> {

    /**
     * 按文件编号查询文件内容
     *
     * @param fileId 文件编号
     * @return 文件内容，不存在时为 null
     */
    default FileContentDO selectByFileId(Long fileId) {
        return selectOne(new LambdaQueryWrapper<FileContentDO>().eq(FileContentDO::getFileId, fileId));
    }

    /**
     * 按文件编号逻辑删除文件内容
     *
     * @param fileId 文件编号
     * @return 删除条数
     */
    default int deleteByFileId(Long fileId) {
        return delete(new LambdaQueryWrapper<FileContentDO>().eq(FileContentDO::getFileId, fileId));
    }
}
